package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.api.LazyInternalApi
import java.util.function.BooleanSupplier

@LazyInternalApi
public data class OutputEntry(
    val port: Int,
    val slot: Int,
    val amount: ResourceAmount<out ResourceVariant>,
)

/** A source is either backed by mutable stores or produces an effectively infinite offer. */
@LazyInternalApi
public interface OutputSource {
    val capabilities: Set<ResourceKind<out ResourceVariant>>
    val finite: Boolean

    fun entries(): List<OutputEntry>

    /** Removes exactly what a destination accepted. Infinite sources intentionally do nothing. */
    fun extract(
        entry: OutputEntry,
        accepted: Long,
    ): Long
}

@LazyInternalApi
public class StoredOutputSource(
    stores: List<ResourceStore<out ResourceVariant>>,
) : OutputSource {
    private val ports: List<OutputPort> = stores.map(::outputPort)

    override val capabilities: Set<ResourceKind<out ResourceVariant>> = stores.mapTo(linkedSetOf()) { it.kind }
    override val finite: Boolean = true

    override fun entries(): List<OutputEntry> =
        buildList {
            ports.forEachIndexed { portIndex, port ->
                port.entries().forEach { (slot, amount) -> add(OutputEntry(portIndex, slot, amount)) }
            }
        }

    override fun extract(
        entry: OutputEntry,
        accepted: Long,
    ): Long =
        if (accepted <= 0L) {
            0L
        } else {
            ports.getOrNull(entry.port)?.extract(entry.slot, entry.amount, accepted) ?: 0L
        }
}

internal class InfiniteOutputSource(
    private val offered: () -> List<ResourceAmount<out ResourceVariant>>,
) : OutputSource {
    override val capabilities: Set<ResourceKind<out ResourceVariant>>
        get() = offered().mapTo(linkedSetOf()) { it.kind }
    override val finite: Boolean = false

    override fun entries(): List<OutputEntry> = offered().mapIndexed { index, amount -> OutputEntry(0, index, amount) }

    override fun extract(
        entry: OutputEntry,
        accepted: Long,
    ): Long = accepted.coerceAtLeast(0L)
}

internal class TransferBudget(
    maximumOffers: Int = DEFAULT_TRANSFER_OFFERS,
) {
    var remainingOffers: Int = maximumOffers.coerceAtLeast(0)
        private set

    val exhausted: Boolean
        get() = remainingOffers <= 0

    fun consume(): Boolean {
        if (remainingOffers <= 0) return false
        remainingOffers--
        return true
    }

    private companion object {
        const val DEFAULT_TRANSFER_OFFERS = 64
    }
}

/** Shared face/network transfer implementation used by every machine output source. */
internal class OutputDispatcher(
    private val origin: BlockPos,
    stillValid: () -> Boolean,
) {
    private val stillValid = BooleanSupplier(stillValid)
    private val faceTransfers = mutableMapOf<ResourceKind<out ResourceVariant>, UntypedResourceFaceTransfer>()
    private var faceCursor = 0
    private var networkCursor = 0

    init {
        BuiltInResourceFaceTransfers.install()
    }

    fun pushToFaces(
        level: ServerLevel,
        source: OutputSource,
        directions: Set<Direction>,
        budget: TransferBudget,
    ): IoPushResult {
        if (directions.isEmpty() || budget.exhausted) return IoPushResult.Success
        val entries = source.entries()
        if (entries.isEmpty()) return IoPushResult.Success
        val orderedDirections = directions.sortedBy(Direction::ordinal)
        val pairs = entries.flatMap { entry -> orderedDirections.map { direction -> entry to direction } }
        val remaining = entries.associate { (it.port to it.slot) to it.amount.amount }.toMutableMap()
        if (pairs.isEmpty()) return IoPushResult.Success

        var visited = 0
        while (visited < pairs.size && !budget.exhausted) {
            val index = (faceCursor + visited) % pairs.size
            val (entry, direction) = pairs[index]
            val key = entry.port to entry.slot
            val available = if (source.finite) remaining[key] ?: 0L else entry.amount.amount
            if (available <= 0L) {
                visited++
                continue
            }
            if (!budget.consume()) break
            val offer = if (available == entry.amount.amount) entry.amount else copyAmount(entry.amount, available)
            val accepted = offerToFace(level, direction, offer)
            if (accepted > 0L) {
                val extracted = source.extract(entry, accepted.coerceAtMost(available))
                if (source.finite) remaining[key] = available - extracted
            }
            visited++
        }
        faceCursor = (faceCursor + visited).mod(pairs.size)
        return IoPushResult.Success
    }

    fun pushToNetwork(
        source: OutputSource,
        target: NetworkTargetRef,
        budget: TransferBudget,
    ): IoPushResult {
        if (budget.exhausted) return IoPushResult.Success
        val entries = source.entries()
        if (entries.isEmpty()) return IoPushResult.Success
        var visited = 0
        var retry = false
        while (visited < entries.size && !budget.exhausted) {
            val index = (networkCursor + visited) % entries.size
            val entry = entries[index]
            if (!budget.consume()) break
            when (val result = target.offer(entry.amount)) {
                is TransferResult.Accepted -> if (result.accepted > 0L) source.extract(entry, result.accepted)
                TransferResult.TemporarilyUnavailable -> retry = true
                TransferResult.TargetMissing, TransferResult.InvalidTarget -> return IoPushResult.TargetMissing
                TransferResult.OutcomeUnknown -> return IoPushResult.OutcomeUnknown
            }
            visited++
        }
        networkCursor = (networkCursor + visited).mod(entries.size)
        return if (retry) IoPushResult.Retry else IoPushResult.Success
    }

    fun invalidate() {
        faceTransfers.values.forEach(UntypedResourceFaceTransfer::invalidate)
        faceTransfers.clear()
    }

    private fun offerToFace(
        level: ServerLevel,
        direction: Direction,
        amount: ResourceAmount<out ResourceVariant>,
    ): Long {
        val transfer =
            faceTransfers[amount.kind]
                ?: ResourceFaceTransferFactories.create(amount.kind, origin, stillValid)?.also {
                    faceTransfers[amount.kind] = it
                }
                ?: return 0L
        return transfer.offer(level, direction, amount)
    }
}

private interface OutputPort {
    fun entries(): List<Pair<Int, ResourceAmount<out ResourceVariant>>>

    fun extract(
        slot: Int,
        expected: ResourceAmount<out ResourceVariant>,
        accepted: Long,
    ): Long
}

private class StoreOutputPort<V : ResourceVariant>(
    private val store: ResourceStore<V>,
) : OutputPort {
    override fun entries(): List<Pair<Int, ResourceAmount<out ResourceVariant>>> =
        buildList {
            repeat(store.slots) { slot ->
                val variant = store.variant(slot) ?: return@repeat
                val amount = store.amount(slot)
                if (amount > 0L) add(slot to ResourceAmount(store.kind, variant, amount))
            }
        }

    override fun extract(
        slot: Int,
        expected: ResourceAmount<out ResourceVariant>,
        accepted: Long,
    ): Long {
        val stored = store.variant(slot) ?: return 0L

        @Suppress("UNCHECKED_CAST")
        val variant = expected.variant as? V ?: return 0L
        if (!store.kind.matches(stored, variant)) return 0L
        return store.extract(slot, accepted)?.amount ?: 0L
    }
}

@Suppress("UNCHECKED_CAST")
private fun outputPort(store: ResourceStore<out ResourceVariant>): OutputPort = StoreOutputPort(store as ResourceStore<ResourceVariant>)

@Suppress("UNCHECKED_CAST")
private fun copyAmount(
    amount: ResourceAmount<out ResourceVariant>,
    value: Long,
): ResourceAmount<out ResourceVariant> {
    val typed = amount as ResourceAmount<ResourceVariant>
    return ResourceAmount(typed.kind, typed.variant, value)
}
