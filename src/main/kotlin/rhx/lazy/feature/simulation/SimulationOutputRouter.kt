package rhx.lazy.feature.simulation

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import net.neoforged.neoforge.items.ItemHandlerHelper
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.IoRoute
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.io.NetworkOutputRouter
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTransferResult
import rhx.lazy.core.storage.LongItemStack
import kotlin.math.min

internal class SimulationOutputRouter(
    private val blockPos: BlockPos,
    val items: MutableList<ItemStack>,
    val fluids: MutableList<FluidStack>,
    val pendingItems: MutableList<LongItemStack>,
    val pendingFluids: MutableList<LongFluidStack>,
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
        while (remaining > 0L) {
            val chunk = min(remaining, Long.MAX_VALUE)
            pendingItems += LongItemStack(stack, chunk)
            remaining -= chunk
        }
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
        while (remaining > 0L) {
            val chunk = min(remaining, Long.MAX_VALUE)
            pendingFluids += LongFluidStack(stack, chunk)
            remaining -= chunk
        }
        changed()
    }

    fun route(
        level: ServerLevel?,
        route: IoRoute,
        target: NetworkTargetRef?,
    ): IoPushResult {
        when (route) {
            IoRoute.NETWORK -> return pushNetwork(target)
            IoRoute.PASSIVE -> movePendingLocal()
            IoRoute.DOWNWARD -> {
                movePendingLocal()
                level?.let(::pushDown)
            }
            IoRoute.ADJACENT -> Unit
        }
        return IoPushResult.Success
    }

    fun normalize() {
        items.resize(ITEM_SLOTS) { ItemStack.EMPTY }
        fluids.resize(FLUID_TANKS) { FluidStack.EMPTY }
    }

    private fun movePendingLocal() {
        var didChange = false
        val itemIterator = pendingItems.listIterator()
        while (itemIterator.hasNext()) {
            val value = itemIterator.next()
            val remainder = insertItemLocal(value.template, value.count)
            if (remainder != value.count) didChange = true
            if (remainder == 0L) itemIterator.remove() else itemIterator.set(value.withCount(remainder))
        }
        val fluidIterator = pendingFluids.listIterator()
        while (fluidIterator.hasNext()) {
            val value = fluidIterator.next()
            val remainder = insertFluidLocal(value.template, value.amount)
            if (remainder != value.amount) didChange = true
            if (remainder == 0L) fluidIterator.remove() else fluidIterator.set(value.withAmount(remainder))
        }
        if (didChange) changed()
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

    private fun pushDown(level: ServerLevel) {
        var didChange = false
        level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos.below(), Direction.UP)?.let { target ->
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
        level.getCapability(Capabilities.FluidHandler.BLOCK, blockPos.below(), Direction.UP)?.let { target ->
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
        if (didChange) changed()
    }

    private fun pushNetwork(target: NetworkTargetRef?): IoPushResult {
        val ref = target ?: return IoPushResult.TargetMissing
        val provider = NetworkOutputProviders.get(ref.providerId) ?: return IoPushResult.Retry
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
                when (val result = NetworkOutputRouter.insert(ref, NetworkPayload.Items(original.template, original.count), false)) {
                    is NetworkTransferResult.Success -> {
                        val remainder = result.remainder.coerceIn(0L, original.count)
                        if (remainder != original.count) didChange = true
                        if (remainder == 0L) {
                            iterator.remove()
                        } else {
                            iterator.set(original.withCount(remainder))
                        }
                    }
                    NetworkTransferResult.TargetMissing, NetworkTransferResult.InvalidTarget -> return finish(IoPushResult.TargetMissing)
                    NetworkTransferResult.OutcomeUnknown -> return finish(IoPushResult.OutcomeUnknown)
                    NetworkTransferResult.TemporarilyUnavailable -> retry = true
                }
            }
            items.indices.forEach { slot ->
                val stack = items[slot]
                if (stack.isEmpty) return@forEach
                when (val result = NetworkOutputRouter.insert(ref, NetworkPayload.Items(stack, stack.count.toLong()), false)) {
                    is NetworkTransferResult.Success -> {
                        val remainder = result.remainder.coerceIn(0L, stack.count.toLong()).toInt()
                        if (remainder != stack.count) {
                            items[slot] = if (remainder == 0) ItemStack.EMPTY else stack.copyWithCount(remainder)
                            didChange = true
                        }
                    }
                    NetworkTransferResult.TemporarilyUnavailable -> retry = true
                    NetworkTransferResult.TargetMissing, NetworkTransferResult.InvalidTarget -> return finish(IoPushResult.TargetMissing)
                    NetworkTransferResult.OutcomeUnknown -> return finish(IoPushResult.OutcomeUnknown)
                }
            }
        } else {
            movePendingItemsLocal()
        }
        if (NetworkInsertCapabilities.FLUID in provider.capabilities) {
            val iterator = pendingFluids.listIterator()
            while (iterator.hasNext()) {
                val original = iterator.next()
                val chunk = min(Int.MAX_VALUE.toLong(), original.amount).toInt()
                when (val result = NetworkOutputRouter.insert(ref, NetworkPayload.Fluid(original.template.copyWithAmount(chunk)), false)) {
                    is NetworkTransferResult.Success -> {
                        val sent = chunk.toLong() - result.remainder.coerceIn(0L, chunk.toLong())
                        val left = original.amount - sent
                        if (sent > 0L) didChange = true
                        if (left == 0L) iterator.remove() else iterator.set(original.withAmount(left))
                    }
                    NetworkTransferResult.TargetMissing, NetworkTransferResult.InvalidTarget -> return finish(IoPushResult.TargetMissing)
                    NetworkTransferResult.OutcomeUnknown -> return finish(IoPushResult.OutcomeUnknown)
                    NetworkTransferResult.TemporarilyUnavailable -> retry = true
                }
            }
            fluids.indices.forEach { tank ->
                val stack = fluids[tank]
                if (stack.isEmpty) return@forEach
                when (val result = NetworkOutputRouter.insert(ref, NetworkPayload.Fluid(stack), false)) {
                    is NetworkTransferResult.Success -> {
                        val remainder = result.remainder.coerceIn(0L, stack.amount.toLong()).toInt()
                        if (remainder != stack.amount) {
                            fluids[tank] = if (remainder == 0) FluidStack.EMPTY else stack.copyWithAmount(remainder)
                            didChange = true
                        }
                    }
                    NetworkTransferResult.TemporarilyUnavailable -> retry = true
                    NetworkTransferResult.TargetMissing, NetworkTransferResult.InvalidTarget -> return finish(IoPushResult.TargetMissing)
                    NetworkTransferResult.OutcomeUnknown -> return finish(IoPushResult.OutcomeUnknown)
                }
            }
        } else {
            movePendingFluidsLocal()
        }
        return finish(if (retry) IoPushResult.Retry else IoPushResult.Success)
    }

    private fun movePendingItemsLocal() {
        val fluidsCopy = pendingFluids.toList()
        pendingFluids.clear()
        movePendingLocal()
        pendingFluids += fluidsCopy
    }

    private fun movePendingFluidsLocal() {
        val itemsCopy = pendingItems.toList()
        pendingItems.clear()
        movePendingLocal()
        pendingItems += itemsCopy
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
