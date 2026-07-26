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
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult
import kotlin.math.min

internal class PlanterOutputRouter(
    private val blockPos: BlockPos,
    private val outputs: MutableList<ItemStack>,
    private val pendingDrops: MutableList<ItemStack>,
    private val networkStorage: NetworkStoragePort,
    private val networkId: () -> NetworkStorageId?,
    private val disableNetworkForwarding: () -> Unit,
    private val markOutputsDirty: () -> Unit,
    private val markPendingDirty: () -> Unit,
) {
    val outputHandler: IItemHandlerModifiable = OutputHandler()

    val hasPendingDrops: Boolean
        get() = pendingDrops.isNotEmpty()

    fun route(
        level: ServerLevel,
        networkForwardingEnabled: Boolean,
        downwardOutputEnabled: Boolean,
    ) {
        if (networkForwardingEnabled) {
            forwardPendingToNetwork()
            forwardOutputsToNetwork()
        }
        movePendingToOutputs()
        if (!downwardOutputEnabled) return

        var hadPending: Boolean
        var pushed: Boolean
        do {
            hadPending = pendingDrops.isNotEmpty()
            pushed = pushOutputsDown(level)
            if (pushed) movePendingToOutputs()
        } while (pushed && hadPending)
    }

    fun enqueue(stack: ItemStack) {
        if (stack.isEmpty) return
        pendingDrops += splitToLegalStacks(stack)
        markPendingDirty()
    }

    fun pendingTooltipTag(registries: HolderLookup.Provider?): CompoundTag {
        val tag = CompoundTag()
        if (registries == null) return tag
        val groups = pendingGroups()
        val entries = ListTag()
        groups.take(MAX_TOOLTIP_GROUPS).forEach { group ->
            val entry = CompoundTag()
            entry.put(PENDING_STACK_TAG, group.template.save(registries))
            entry.putLong(PENDING_COUNT_TAG, group.count)
            entries.add(entry)
        }
        tag.put(PENDING_ENTRIES_TAG, entries)
        tag.putInt(PENDING_REMAINING_TYPES_TAG, (groups.size - MAX_TOOLTIP_GROUPS).coerceAtLeast(0))
        return tag
    }

    fun takeAllForDrop(): List<ItemStack> {
        val drops =
            buildList {
                outputs.filterNot(ItemStack::isEmpty).forEach { add(it.copy()) }
                pendingDrops.filterNot(ItemStack::isEmpty).forEach { add(it.copy()) }
            }
        repeat(outputs.size) { outputs[it] = ItemStack.EMPTY }
        pendingDrops.clear()
        markOutputsDirty()
        markPendingDirty()
        return drops
    }

    fun normalizeAfterLoad() {
        outputs.resize(OUTPUT_SLOT_COUNT)
        outputs.indices.forEach { slot ->
            outputs[slot] = normalizeOutput(outputs[slot])
        }
        val normalizedPending = pendingDrops.filterNot(ItemStack::isEmpty).flatMap(::splitToLegalStacks)
        pendingDrops.clear()
        pendingDrops.addAll(normalizedPending)
    }

    internal fun forwardPendingToNetwork() {
        var changed = false
        var slot = 0
        while (slot < pendingDrops.size && networkId() != null) {
            val original = pendingDrops[slot]
            val remainder = forwardToNetwork(original)
            if (!ItemStack.matches(original, remainder)) {
                changed = true
            }
            if (remainder.isEmpty) {
                pendingDrops.removeAt(slot)
            } else {
                pendingDrops[slot] = remainder
                slot++
            }
        }
        if (changed) markPendingDirty()
    }

    private fun forwardOutputsToNetwork() {
        var changed = false
        for (slot in outputs.indices) {
            if (networkId() == null) break
            val original = outputs[slot]
            if (original.isEmpty) continue
            val remainder = forwardToNetwork(original)
            if (!ItemStack.matches(original, remainder)) {
                outputs[slot] = remainder
                changed = true
            }
        }
        if (changed) markOutputsDirty()
    }

    private fun forwardToNetwork(stack: ItemStack): ItemStack {
        val targetNetwork = networkId() ?: return stack
        return when (val result = networkStorage.insertItem(targetNetwork, stack, simulate = false)) {
            is NetworkStorageResult.Success -> normalizedRemainder(stack, result.value)
            else -> {
                disableNetworkForwarding()
                stack
            }
        }
    }

    private fun movePendingToOutputs() {
        var changedPending = false
        var changedOutputs = false
        var index = 0
        while (index < pendingDrops.size) {
            val original = pendingDrops[index]
            val remainder = insertIntoOutputs(original)
            if (!ItemStack.matches(original, remainder)) {
                changedOutputs = true
                changedPending = true
            }
            if (remainder.isEmpty) {
                pendingDrops.removeAt(index)
            } else {
                pendingDrops[index] = remainder
                index++
            }
        }
        if (changedPending) markPendingDirty()
        if (changedOutputs) markOutputsDirty()
    }

    private fun insertIntoOutputs(stack: ItemStack): ItemStack {
        var remaining = stack.copy()
        val matching =
            outputs.indices.filter { slot ->
                val stored = outputs[slot]
                !stored.isEmpty && ItemStack.isSameItemSameComponents(stored, remaining)
            }
        val empty = outputs.indices.filter { outputs[it].isEmpty }
        for (slot in matching + empty) {
            if (remaining.isEmpty) break
            val stored = outputs[slot]
            val capacity = min(NORMAL_SLOT_LIMIT, remaining.maxStackSize.coerceAtLeast(1)) - stored.count
            if (capacity <= 0) continue
            val inserted = min(capacity, remaining.count)
            outputs[slot] =
                if (stored.isEmpty) {
                    remaining.copyWithCount(inserted)
                } else {
                    stored.copyWithCount(stored.count + inserted)
                }
            remaining =
                if (inserted == remaining.count) {
                    ItemStack.EMPTY
                } else {
                    remaining.copyWithCount(remaining.count - inserted)
                }
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

    private fun pendingGroups(): List<PendingGroup> {
        val groups = mutableListOf<PendingGroup>()
        pendingDrops.forEach { stack ->
            val group = groups.firstOrNull { ItemStack.isSameItemSameComponents(it.template, stack) }
            if (group == null) {
                groups += PendingGroup(stack.copyWithCount(1), stack.count.toLong())
            } else {
                group.count += stack.count.toLong()
            }
        }
        return groups
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
                outputs[slot] =
                    if (remaining == 0) {
                        ItemStack.EMPTY
                    } else {
                        stored.copyWithCount(remaining)
                    }
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
            if (
                !normalized.isEmpty &&
                (
                    stored.isEmpty ||
                        !ItemStack.isSameItemSameComponents(stored, normalized) ||
                        normalized.count > stored.count
                )
            ) {
                return
            }
            if (ItemStack.matches(stored, normalized)) return
            outputs[slot] = normalized
            markOutputsDirty()
        }
    }

    private data class PendingGroup(
        val template: ItemStack,
        var count: Long,
    )

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

        private fun splitToLegalStacks(stack: ItemStack): List<ItemStack> {
            var remaining = stack.count
            val split = mutableListOf<ItemStack>()
            val maxSize = stack.maxStackSize.coerceAtLeast(1)
            while (remaining > 0) {
                val amount = min(remaining, maxSize)
                split += stack.copyWithCount(amount)
                remaining -= amount
            }
            return split
        }

        private fun normalizedRemainder(
            original: ItemStack,
            returned: ItemStack,
        ): ItemStack {
            if (returned.isEmpty) return ItemStack.EMPTY
            if (!ItemStack.isSameItemSameComponents(original, returned)) return original
            return original.copyWithCount(returned.count.coerceIn(0, original.count))
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
