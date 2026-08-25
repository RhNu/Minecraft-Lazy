package rhx.lazy.feature.simulation

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandlerModifiable
import kotlin.math.min

internal class SimulationInputHandler(
    private val stacks: List<ItemStack>,
    private val slotLimit: (Int) -> Int,
    private val validInput: (Int, ItemStack) -> Boolean,
    private val update: (Int, ItemStack) -> Unit,
) : IItemHandlerModifiable {
    override fun getSlots() = stacks.size

    override fun getStackInSlot(slot: Int) = stacks[slot].copy()

    override fun insertItem(
        slot: Int,
        stack: ItemStack,
        simulate: Boolean,
    ): ItemStack {
        if (stack.isEmpty || !validInput(slot, stack)) return stack
        val stored = stacks[slot]
        if (!stored.isEmpty && !ItemStack.isSameItemSameComponents(stored, stack)) return stack
        val accepted = min(stack.count, effectiveLimit(slot, stack) - stored.count)
        if (accepted <= 0) return stack
        if (!simulate) {
            update(
                slot,
                if (stored.isEmpty) stack.copyWithCount(accepted) else stored.copyWithCount(stored.count + accepted),
            )
        }
        return if (accepted == stack.count) ItemStack.EMPTY else stack.copyWithCount(stack.count - accepted)
    }

    override fun extractItem(
        slot: Int,
        amount: Int,
        simulate: Boolean,
    ): ItemStack {
        val stored = stacks[slot]
        if (stored.isEmpty || amount <= 0) return ItemStack.EMPTY
        val result = stored.copyWithCount(min(amount, stored.count))
        if (!simulate) {
            update(
                slot,
                if (result.count == stored.count) ItemStack.EMPTY else stored.copyWithCount(stored.count - result.count),
            )
        }
        return result
    }

    override fun getSlotLimit(slot: Int) = slotLimit(slot).coerceAtLeast(1)

    override fun isItemValid(
        slot: Int,
        stack: ItemStack,
    ) = validInput(slot, stack)

    override fun setStackInSlot(
        slot: Int,
        stack: ItemStack,
    ) {
        if (stack.isEmpty || validInput(slot, stack)) {
            val normalized =
                if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(min(stack.count, effectiveLimit(slot, stack)))
            update(slot, normalized)
        }
    }

    private fun effectiveLimit(
        slot: Int,
        stack: ItemStack,
    ) = min(getSlotLimit(slot), stack.maxStackSize.coerceAtLeast(1))
}
