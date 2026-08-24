package rhx.lazy.core.process

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceBundle
import rhx.lazy.core.resource.ResourceDelta
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.core.resource.ResourceTransaction
import rhx.lazy.core.resource.StoreDelta

internal enum class WorkStatus {
    IDLE,
    RUNNING,
    BLOCKED,
    FAULTED,
}

internal sealed interface WorkStep {
    data object Idle : WorkStep

    data object Running : WorkStep

    data class Produced(
        val commit: PreparedCommit,
    ) : WorkStep

    data class Faulted(
        val reason: String,
    ) : WorkStep
}

internal interface WorkProvider {
    fun step(workBudget: Int): WorkStep

    fun committed(workUnits: Int)
}

/**
 * The only work result allowed to survive a failed commit. Its identities and random quantities are
 * immutable; draining only advances the persisted remainder.
 */
internal class PreparedCommit(
    items: List<ResourceAmount<ItemVariant>>,
    fluids: List<ResourceAmount<FluidVariant>>,
    val workUnits: Int,
) {
    private val pendingItems = items.mapTo(mutableListOf()) { ResourceAmount(ItemResourceKind, it.variant, it.amount) }
    private val pendingFluids = fluids.mapTo(mutableListOf()) { ResourceAmount(FluidResourceKind, it.variant, it.amount) }

    init {
        require(workUnits > 0) { "A prepared commit must represent positive work" }
    }

    val isComplete: Boolean
        get() = pendingItems.isEmpty() && pendingFluids.isEmpty()

    val bundle: ResourceBundle
        get() = ResourceBundle.of(pendingItems + pendingFluids)

    /**
     * First attempts a single all-or-nothing transaction per resource kind. If diversity prevents
     * that, it advances individual entries so a result larger than the store's kind count can be
     * drained without ever rerolling it.
     */
    fun drainInto(
        items: ResourceStore<ItemVariant>,
        fluids: ResourceStore<FluidVariant>,
    ): Boolean {
        val appliedAtomically =
            when {
                pendingItems.isNotEmpty() && pendingFluids.isNotEmpty() ->
                    ResourceTransaction.tryApply(
                        StoreDelta(items, ResourceDelta(inserted = pendingItems)),
                        StoreDelta(fluids, ResourceDelta(inserted = pendingFluids)),
                    )
                pendingItems.isNotEmpty() -> items.tryApply(ResourceDelta(inserted = pendingItems))
                pendingFluids.isNotEmpty() -> fluids.tryApply(ResourceDelta(inserted = pendingFluids))
                else -> true
            }
        if (appliedAtomically) {
            pendingItems.clear()
            pendingFluids.clear()
            return true
        }
        drain(pendingItems, items)
        drain(pendingFluids, fluids)
        return isComplete
    }

    fun save(registries: HolderLookup.Provider): CompoundTag =
        CompoundTag().apply {
            putInt(WORK_UNITS_TAG, workUnits)
            put(ITEMS_TAG, saveEntries(registries, pendingItems))
            put(FLUIDS_TAG, saveEntries(registries, pendingFluids))
        }

    private fun <V : rhx.lazy.core.resource.ResourceVariant> drain(
        pending: MutableList<ResourceAmount<V>>,
        store: ResourceStore<V>,
    ) {
        val iterator = pending.listIterator()
        while (iterator.hasNext()) {
            val amount = iterator.next()
            val accepted = store.insert(amount)
            when {
                accepted == amount.amount -> iterator.remove()
                accepted > 0L -> iterator.set(amount.withAmount(amount.amount - accepted))
                else -> Unit
            }
        }
    }

    private fun <V : rhx.lazy.core.resource.ResourceVariant> saveEntries(
        registries: HolderLookup.Provider,
        entries: List<ResourceAmount<V>>,
    ): ListTag =
        ListTag().apply {
            entries.forEach { entry ->
                add(
                    CompoundTag().apply {
                        put(VARIANT_TAG, entry.kind.save(registries, entry.variant))
                        putLong(AMOUNT_TAG, entry.amount)
                    },
                )
            }
        }

    companion object {
        fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): PreparedCommit? {
            val workUnits = tag.getInt(WORK_UNITS_TAG)
            if (workUnits <= 0) return null
            val items = parseEntries(registries, tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND.toInt()), ItemResourceKind)
            val fluids = parseEntries(registries, tag.getList(FLUIDS_TAG, Tag.TAG_COMPOUND.toInt()), FluidResourceKind)
            if (items.isEmpty() && fluids.isEmpty()) return null
            return PreparedCommit(items, fluids, workUnits)
        }

        private fun <V : rhx.lazy.core.resource.ResourceVariant> parseEntries(
            registries: HolderLookup.Provider,
            list: ListTag,
            kind: rhx.lazy.core.resource.ResourceKind<V>,
        ): List<ResourceAmount<V>> =
            buildList {
                list.forEach { raw ->
                    val entry = raw as? CompoundTag ?: return@forEach
                    val amount = entry.getLong(AMOUNT_TAG)
                    if (amount <= 0L) return@forEach
                    val variantTag = entry.get(VARIANT_TAG) as? CompoundTag ?: return@forEach
                    val variant = kind.parse(registries, variantTag) ?: return@forEach
                    add(ResourceAmount(kind, variant, amount))
                }
            }

        private const val WORK_UNITS_TAG = "workUnits"
        private const val ITEMS_TAG = "items"
        private const val FLUIDS_TAG = "fluids"
        private const val VARIANT_TAG = "variant"
        private const val AMOUNT_TAG = "amount"
    }
}

/** Owns lifecycle state and guarantees that no new work is generated behind a failed commit. */
internal class WorkController(
    private val provider: WorkProvider,
    private val commit: (PreparedCommit) -> Boolean,
) {
    var status: WorkStatus = WorkStatus.IDLE
        private set

    var preparedCommit: PreparedCommit? = null
        private set

    var faultReason: String? = null
        private set

    fun tick(workBudget: Int) {
        if (status == WorkStatus.FAULTED) return
        val pending = preparedCommit
        if (pending != null) {
            if (!commit(pending)) {
                status = WorkStatus.BLOCKED
                return
            }
            preparedCommit = null
            provider.committed(pending.workUnits)
        }

        when (val step = provider.step(workBudget.coerceAtLeast(0))) {
            WorkStep.Idle -> status = WorkStatus.IDLE
            WorkStep.Running -> status = WorkStatus.RUNNING
            is WorkStep.Faulted -> {
                faultReason = step.reason
                status = WorkStatus.FAULTED
            }
            is WorkStep.Produced -> {
                if (commit(step.commit)) {
                    provider.committed(step.commit.workUnits)
                    status = WorkStatus.RUNNING
                } else {
                    preparedCommit = step.commit
                    status = WorkStatus.BLOCKED
                }
            }
        }
    }

    fun restore(commit: PreparedCommit?) {
        preparedCommit = commit
        status = if (commit == null) WorkStatus.IDLE else WorkStatus.BLOCKED
        faultReason = null
    }

    fun clear() {
        preparedCommit = null
        faultReason = null
        status = WorkStatus.IDLE
    }
}
