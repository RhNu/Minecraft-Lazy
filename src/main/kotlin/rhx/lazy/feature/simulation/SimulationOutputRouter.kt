package rhx.lazy.feature.simulation

import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import net.neoforged.neoforge.items.ItemHandlerHelper
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NeighborCapabilities
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOffer
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.offer
import rhx.lazy.core.storage.LongItemStack
import kotlin.math.min

/**
 * Buffers a simulation batch's oversized results and drains them into the machine's own slots, its
 * neighbours or a network target, depending on which push [rhx.lazy.core.io.IoController] asks for.
 */
internal class SimulationOutputRouter(
    val items: MutableList<ItemStack>,
    val fluids: MutableList<FluidStack>,
    val pendingItems: MutableList<LongItemStack>,
    val pendingFluids: MutableList<LongFluidStack>,
    private val neighborItems: NeighborCapabilities<IItemHandler>,
    private val neighborFluids: NeighborCapabilities<IFluidHandler>,
    private val changed: () -> Unit,
) {
    val itemHandler: IItemHandlerModifiable = SimulationOutputItemHandler(items, changed)
    val fluidHandler: IFluidHandler = SimulationOutputFluidHandler(fluids, TANK_CAPACITY, changed)
    val hasPending: Boolean
        get() = pendingItems.isNotEmpty() || pendingFluids.isNotEmpty()
    val hasOutputs: Boolean
        get() = hasPending || items.any { !it.isEmpty } || fluids.any { !it.isEmpty }

    fun enqueue(
        stack: ItemStack,
        amount: Long = stack.count.toLong(),
    ) {
        if (stack.isEmpty || amount <= 0) return
        var remaining = amount
        pendingItems.indices
            .filter { pendingItems[it].matches(stack) && pendingItems[it].count < Long.MAX_VALUE }
            .forEach { index ->
                if (remaining <= 0L) return@forEach
                val current = pendingItems[index]
                val accepted = min(remaining, Long.MAX_VALUE - current.count)
                pendingItems[index] = current.withCount(current.count + accepted)
                remaining -= accepted
            }
        if (remaining > 0L) pendingItems += LongItemStack(stack, remaining)
        changed()
    }

    fun enqueue(
        stack: FluidStack,
        amount: Long = stack.amount.toLong(),
    ) {
        if (stack.isEmpty || amount <= 0) return
        var remaining = amount
        pendingFluids.indices
            .filter { pendingFluids[it].matches(stack) && pendingFluids[it].amount < Long.MAX_VALUE }
            .forEach { index ->
                if (remaining <= 0L) return@forEach
                val current = pendingFluids[index]
                val accepted = min(remaining, Long.MAX_VALUE - current.amount)
                pendingFluids[index] = current.withAmount(current.amount + accepted)
                remaining -= accepted
            }
        if (remaining > 0L) pendingFluids += LongFluidStack(stack, remaining)
        changed()
    }

    fun normalize() {
        items.resize(ITEM_SLOTS) { ItemStack.EMPTY }
        fluids.resize(FLUID_TANKS) { FluidStack.EMPTY }
    }

    /** Drains the oversized backlog into the machine's own slots; safe to run every tick. */
    fun movePendingLocal() {
        val didItemsChange = drainPendingItems()
        val didFluidsChange = drainPendingFluids()
        if (didItemsChange || didFluidsChange) changed()
    }

    private fun drainPendingItems(): Boolean {
        var didChange = false
        val iterator = pendingItems.listIterator()
        while (iterator.hasNext()) {
            val value = iterator.next()
            val remainder = insertItemLocal(value.template, value.count)
            if (remainder != value.count) didChange = true
            if (remainder == 0L) iterator.remove() else iterator.set(value.withCount(remainder))
        }
        return didChange
    }

    private fun drainPendingFluids(): Boolean {
        var didChange = false
        val iterator = pendingFluids.listIterator()
        while (iterator.hasNext()) {
            val value = iterator.next()
            val remainder = insertFluidLocal(value.template, value.amount)
            if (remainder != value.amount) didChange = true
            if (remainder == 0L) iterator.remove() else iterator.set(value.withAmount(remainder))
        }
        return didChange
    }

    private fun insertItemLocal(
        template: ItemStack,
        amount: Long,
    ): Long {
        var remaining = amount
        val limit = min(64, template.maxStackSize.coerceAtLeast(1))

        fun insert(slot: Int) {
            val stored = items[slot]
            val accepted = min(remaining, (limit - stored.count).coerceAtLeast(0).toLong()).toInt()
            if (accepted <= 0) return
            items[slot] = if (stored.isEmpty) template.copyWithCount(accepted) else stored.copyWithCount(stored.count + accepted)
            remaining -= accepted
        }
        items.indices.filter { !items[it].isEmpty && ItemStack.isSameItemSameComponents(items[it], template) }.forEach(::insert)
        items.indices.filter { items[it].isEmpty }.forEach(::insert)
        return remaining
    }

    private fun insertFluidLocal(
        template: FluidStack,
        amount: Long,
    ): Long {
        var remaining = amount

        fun insert(tank: Int) {
            val stored = fluids[tank]
            val accepted = min(remaining, (TANK_CAPACITY - stored.amount).coerceAtLeast(0).toLong()).toInt()
            if (accepted <= 0) return
            fluids[tank] = if (stored.isEmpty) template.copyWithAmount(accepted) else stored.copyWithAmount(stored.amount + accepted)
            remaining -= accepted
        }
        fluids.indices.filter { !fluids[it].isEmpty && FluidStack.isSameFluidSameComponents(fluids[it], template) }.forEach(::insert)
        fluids.indices.filter { fluids[it].isEmpty }.forEach(::insert)
        return remaining
    }

    fun pushToFaces(
        level: ServerLevel,
        directions: Set<Direction>,
    ): IoPushResult {
        var didChange = false
        directions.forEach { direction ->
            neighborItems[level, direction]?.let { target ->
                items.indices.forEach { slot ->
                    val original = items[slot]
                    if (original.isEmpty) return@forEach
                    val remainder = ItemHandlerHelper.insertItemStacked(target, original, false)
                    if (!ItemStack.matches(original, remainder)) {
                        items[slot] = remainder
                        didChange = true
                    }
                }
            }
        }
        directions.forEach { direction ->
            neighborFluids[level, direction]?.let { target ->
                fluids.indices.forEach { tank ->
                    val original = fluids[tank]
                    if (original.isEmpty) return@forEach
                    val accepted = target.fill(original, IFluidHandler.FluidAction.EXECUTE)
                    if (accepted > 0) {
                        fluids[tank] =
                            if (accepted >= original.amount) FluidStack.EMPTY else original.copyWithAmount(original.amount - accepted)
                        didChange = true
                    }
                }
            }
        }
        if (didChange) changed()
        return IoPushResult.Success
    }

    /**
     * Sends everything the target's provider can accept; whatever it cannot handle falls back to the
     * machine's own slots so a fluid-blind network never stalls the item backlog and vice versa.
     */
    fun pushToNetwork(target: NetworkTargetRef): IoPushResult {
        val provider = NetworkOutputProviders.get(target.providerId) ?: return IoPushResult.Retry
        var retry = false
        var didChange = false

        fun finish(result: IoPushResult): IoPushResult {
            if (didChange) changed()
            return result
        }

        if (NetworkInsertCapabilities.ITEM in provider.capabilities) {
            val iterator = pendingItems.listIterator()
            while (iterator.hasNext()) {
                val original = iterator.next()
                when (val offer = target.offer(NetworkPayload.Items(original.template, original.count), original.count)) {
                    is NetworkOffer.Accepted -> {
                        val left = original.count - offer.accepted
                        if (offer.accepted > 0L) didChange = true
                        if (left == 0L) iterator.remove() else iterator.set(original.withCount(left))
                    }
                    is NetworkOffer.Rejected ->
                        if (offer.push == IoPushResult.Retry) retry = true else return finish(offer.push)
                }
            }
            items.indices.forEach { slot ->
                val stack = items[slot]
                if (stack.isEmpty) return@forEach
                when (val offer = target.offer(NetworkPayload.Items(stack, stack.count.toLong()), stack.count.toLong())) {
                    is NetworkOffer.Accepted -> {
                        if (offer.accepted > 0L) {
                            val left = stack.count - offer.accepted.toInt()
                            items[slot] = if (left <= 0) ItemStack.EMPTY else stack.copyWithCount(left)
                            didChange = true
                        }
                    }
                    is NetworkOffer.Rejected ->
                        if (offer.push == IoPushResult.Retry) retry = true else return finish(offer.push)
                }
            }
        } else if (drainPendingItems()) {
            didChange = true
        }

        if (NetworkInsertCapabilities.FLUID in provider.capabilities) {
            val iterator = pendingFluids.listIterator()
            while (iterator.hasNext()) {
                val original = iterator.next()
                val chunk = min(Int.MAX_VALUE.toLong(), original.amount).toInt()
                val payload = NetworkPayload.Fluid(original.template.copyWithAmount(chunk))
                when (val offer = target.offer(payload, chunk.toLong())) {
                    is NetworkOffer.Accepted -> {
                        val left = original.amount - offer.accepted
                        if (offer.accepted > 0L) didChange = true
                        if (left == 0L) iterator.remove() else iterator.set(original.withAmount(left))
                    }
                    is NetworkOffer.Rejected ->
                        if (offer.push == IoPushResult.Retry) retry = true else return finish(offer.push)
                }
            }
            fluids.indices.forEach { tank ->
                val stack = fluids[tank]
                if (stack.isEmpty) return@forEach
                when (val offer = target.offer(NetworkPayload.Fluid(stack), stack.amount.toLong())) {
                    is NetworkOffer.Accepted -> {
                        if (offer.accepted > 0L) {
                            val left = stack.amount - offer.accepted.toInt()
                            fluids[tank] = if (left <= 0) FluidStack.EMPTY else stack.copyWithAmount(left)
                            didChange = true
                        }
                    }
                    is NetworkOffer.Rejected ->
                        if (offer.push == IoPushResult.Retry) retry = true else return finish(offer.push)
                }
            }
        } else if (drainPendingFluids()) {
            didChange = true
        }
        return finish(if (retry) IoPushResult.Retry else IoPushResult.Success)
    }

    companion object {
        const val ITEM_SLOTS = 16
        const val FLUID_TANKS = MAX_OUTPUT_ENTRIES
        const val TANK_CAPACITY = Int.MAX_VALUE
    }
}

private fun <T> MutableList<T>.resize(
    size: Int,
    factory: () -> T,
) {
    while (this.size > size) removeAt(lastIndex)
    while (this.size < size) add(factory())
}
