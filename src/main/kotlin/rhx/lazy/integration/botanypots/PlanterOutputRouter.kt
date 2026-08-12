package rhx.lazy.integration.botanypots

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandlerModifiable
import net.neoforged.neoforge.items.ItemHandlerHelper
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.IoRoute
import rhx.lazy.core.io.NetworkOutputRouter
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTransferResult
import rhx.lazy.core.storage.LongItemStack
import kotlin.math.min

internal class PlanterOutputRouter(
    private val blockPos: BlockPos,
    private val outputs: MutableList<ItemStack>,
    private val pendingDrops: MutableList<LongItemStack>,
    private val markOutputsDirty: () -> Unit,
    private val markPendingDirty: () -> Unit,
) {
    val outputHandler: IItemHandlerModifiable = OutputHandler()

    val hasPendingDrops: Boolean
        get() = pendingDrops.isNotEmpty()

    fun route(
        level: ServerLevel?,
        route: IoRoute,
        target: NetworkTargetRef?,
    ): IoPushResult =
        when (route) {
            IoRoute.PASSIVE -> {
                movePendingToOutputs()
                IoPushResult.Success
            }

            IoRoute.DOWNWARD -> {
                val serverLevel = level ?: return IoPushResult.Retry
                movePendingToOutputs()
                pushOutputsDown(serverLevel)
                IoPushResult.Success
            }

            IoRoute.NETWORK -> pushToNetwork(target)
            IoRoute.ADJACENT -> IoPushResult.Success
        }

    internal fun routeNetwork(target: NetworkTargetRef): IoPushResult = pushToNetwork(target)

    fun enqueue(stack: ItemStack) {
        if (addPending(stack, stack.count.toLong())) markPendingDirty()
    }

    fun enqueueBatch(produce: ((ItemStack) -> Unit) -> Unit) {
        var changed = false
        produce { stack ->
            changed = addPending(stack, stack.count.toLong()) || changed
        }
        if (changed) markPendingDirty()
    }

    fun pendingTooltipTag(registries: HolderLookup.Provider?): CompoundTag {
        val tag = CompoundTag()
        if (registries == null) return tag
        val entries = ListTag()
        pendingDrops.take(MAX_TOOLTIP_GROUPS).forEach { pending ->
            val entry = CompoundTag()
            entry.put(PENDING_STACK_TAG, pending.template.save(registries))
            entry.putLong(PENDING_COUNT_TAG, pending.count)
            entries.add(entry)
        }
        tag.put(PENDING_ENTRIES_TAG, entries)
        tag.putInt(PENDING_REMAINING_TYPES_TAG, (pendingDrops.size - MAX_TOOLTIP_GROUPS).coerceAtLeast(0))
        return tag
    }

    fun takeAllForDrop(drop: (ItemStack) -> Unit) {
        outputs.filterNot(ItemStack::isEmpty).forEach { drop(it.copy()) }
        pendingDrops.forEach { pending -> pending.toItemStacks().forEach(drop) }
        repeat(outputs.size) { outputs[it] = ItemStack.EMPTY }
        pendingDrops.clear()
        markOutputsDirty()
        markPendingDirty()
    }

    fun normalizeAfterLoad() {
        outputs.resize(OUTPUT_SLOT_COUNT)
        outputs.indices.forEach { slot ->
            outputs[slot] = normalizeOutput(outputs[slot])
        }
        val loaded = pendingDrops.toList()
        pendingDrops.clear()
        loaded.forEach { pending -> addPending(pending.template, pending.count) }
    }

    private fun addPending(
        stack: ItemStack,
        amount: Long,
    ): Boolean {
        if (stack.isEmpty || amount <= 0L) return false
        val index = pendingDrops.indexOfFirst { it.matches(stack) }
        if (index >= 0) {
            pendingDrops[index] = pendingDrops[index].plus(amount)
        } else {
            pendingDrops += LongItemStack(stack, amount)
        }
        return true
    }

    private fun pushToNetwork(target: NetworkTargetRef?): IoPushResult {
        val networkTarget = target ?: return IoPushResult.TargetMissing
        var pendingChanged = false
        var outputsChanged = false

        fun handle(
            template: ItemStack,
            amount: Long,
        ): NetworkTransferResult =
            NetworkOutputRouter.insert(
                networkTarget,
                NetworkPayload.Items(template.copyWithCount(1), amount),
                false,
            )

        fun finish(result: IoPushResult): IoPushResult {
            if (pendingChanged) markPendingDirty()
            if (outputsChanged) markOutputsDirty()
            return result
        }

        var pendingIndex = 0
        while (pendingIndex < pendingDrops.size) {
            val original = pendingDrops[pendingIndex]
            when (val result = handle(original.template, original.count)) {
                is NetworkTransferResult.Success -> {
                    val remainder = result.remainder.coerceIn(0L, original.count)
                    if (remainder != original.count) pendingChanged = true
                    if (remainder == 0L) {
                        pendingDrops.removeAt(pendingIndex)
                    } else {
                        pendingDrops[pendingIndex] = original.withCount(remainder)
                        pendingIndex++
                    }
                }

                NetworkTransferResult.TargetMissing,
                NetworkTransferResult.InvalidTarget,
                -> return finish(IoPushResult.TargetMissing)

                NetworkTransferResult.OutcomeUnknown -> return finish(IoPushResult.OutcomeUnknown)
                NetworkTransferResult.TemporarilyUnavailable -> return finish(IoPushResult.Retry)
            }
        }

        for (slot in outputs.indices) {
            val original = outputs[slot]
            if (original.isEmpty) continue
            when (val result = handle(original, original.count.toLong())) {
                is NetworkTransferResult.Success -> {
                    val remainder = result.remainder.coerceIn(0L, original.count.toLong()).toInt()
                    if (remainder != original.count) {
                        outputs[slot] = if (remainder == 0) ItemStack.EMPTY else original.copyWithCount(remainder)
                        outputsChanged = true
                    }
                }

                NetworkTransferResult.TargetMissing,
                NetworkTransferResult.InvalidTarget,
                -> return finish(IoPushResult.TargetMissing)

                NetworkTransferResult.OutcomeUnknown -> return finish(IoPushResult.OutcomeUnknown)
                NetworkTransferResult.TemporarilyUnavailable -> return finish(IoPushResult.Retry)
            }
        }
        return finish(IoPushResult.Success)
    }

    private fun movePendingToOutputs() {
        var pendingChanged = false
        var outputsChanged = false
        var index = 0
        while (index < pendingDrops.size) {
            val original = pendingDrops[index]
            val remainder = insertIntoOutputs(original.template, original.count)
            if (remainder != original.count) {
                outputsChanged = true
                pendingChanged = true
            }
            if (remainder == 0L) {
                pendingDrops.removeAt(index)
            } else {
                pendingDrops[index] = original.withCount(remainder)
                index++
            }
        }
        if (pendingChanged) markPendingDirty()
        if (outputsChanged) markOutputsDirty()
    }

    private fun insertIntoOutputs(
        template: ItemStack,
        amount: Long,
    ): Long {
        var remaining = amount
        val maxStackSize = min(NORMAL_SLOT_LIMIT, template.maxStackSize.coerceAtLeast(1))

        fun insertInto(slot: Int) {
            if (remaining <= 0L) return
            val stored = outputs[slot]
            val capacity = maxStackSize - stored.count
            if (capacity <= 0) return
            val inserted = min(remaining, capacity.toLong()).toInt()
            outputs[slot] =
                if (stored.isEmpty) {
                    template.copyWithCount(inserted)
                } else {
                    stored.copyWithCount(stored.count + inserted)
                }
            remaining -= inserted.toLong()
        }

        outputs.indices.forEach { slot ->
            val stored = outputs[slot]
            if (!stored.isEmpty && ItemStack.isSameItemSameComponents(stored, template)) insertInto(slot)
        }
        outputs.indices.forEach { slot ->
            if (outputs[slot].isEmpty) insertInto(slot)
        }
        return remaining
    }

    private fun pushOutputsDown(level: ServerLevel): Boolean {
        val target =
            level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                blockPos.below(),
                Direction.UP,
            ) ?: return false
        var changed = false
        outputs.indices.forEach { slot ->
            val stack = outputs[slot]
            if (stack.isEmpty) return@forEach
            val remainder = ItemHandlerHelper.insertItemStacked(target, stack, false)
            if (!ItemStack.matches(stack, remainder)) {
                outputs[slot] = remainder
                changed = true
            }
        }
        if (changed) markOutputsDirty()
        return changed
    }

    private inner class OutputHandler : IItemHandlerModifiable {
        override fun getSlots(): Int = OUTPUT_SLOT_COUNT

        override fun getStackInSlot(slot: Int): ItemStack {
            validateOutputSlot(slot)
            val stack = outputs[slot]
            return if (stack.isEmpty) ItemStack.EMPTY else stack.copy()
        }

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            validateOutputSlot(slot)
            return stack
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            validateOutputSlot(slot)
            val stored = outputs[slot]
            if (amount <= 0 || stored.isEmpty) return ItemStack.EMPTY
            val extracted = min(amount, stored.count)
            val result = stored.copyWithCount(extracted)
            if (!simulate) {
                val remaining = stored.count - extracted
                outputs[slot] = if (remaining == 0) ItemStack.EMPTY else stored.copyWithCount(remaining)
                markOutputsDirty()
            }
            return result
        }

        override fun getSlotLimit(slot: Int): Int {
            validateOutputSlot(slot)
            val stored = outputs[slot]
            return if (stored.isEmpty) NORMAL_SLOT_LIMIT else min(NORMAL_SLOT_LIMIT, stored.maxStackSize)
        }

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean {
            validateOutputSlot(slot)
            return false
        }

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) {
            validateOutputSlot(slot)
            val stored = outputs[slot]
            val normalized = normalizeOutput(stack)
            if (ItemStack.matches(stored, normalized)) return
            outputs[slot] = normalized
            markOutputsDirty()
        }
    }

    companion object {
        const val OUTPUT_SLOT_COUNT = 12

        internal const val PENDING_ENTRIES_TAG = "entries"
        internal const val PENDING_STACK_TAG = "stack"
        internal const val PENDING_COUNT_TAG = "count"
        internal const val PENDING_REMAINING_TYPES_TAG = "remaining_types"

        private const val MAX_TOOLTIP_GROUPS = 16
        private const val NORMAL_SLOT_LIMIT = 64

        private fun validateOutputSlot(slot: Int) {
            if (slot !in 0 until OUTPUT_SLOT_COUNT) {
                throw IndexOutOfBoundsException("Output slot $slot is out of range for planter")
            }
        }

        private fun normalizeOutput(stack: ItemStack): ItemStack =
            if (stack.isEmpty) {
                ItemStack.EMPTY
            } else {
                stack.copyWithCount(
                    stack.count.coerceIn(
                        1,
                        min(NORMAL_SLOT_LIMIT, stack.maxStackSize.coerceAtLeast(1)),
                    ),
                )
            }
    }
}

private fun MutableList<ItemStack>.resize(size: Int) {
    while (this.size > size) {
        removeAt(lastIndex)
    }
    while (this.size < size) {
        add(ItemStack.EMPTY)
    }
}
