package rhx.lazy.core.ui

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.IItemHandlerModifiable
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LargeItemHandlerSlotTest {
    @Test
    fun `slot accepts normal cursor stacks above the item's ordinary stack limit`() {
        val handler = TestLargeHandler(ItemStack(Items.STONE, 256))
        val slot = LargeItemHandlerSlot(handler, 0)

        assertEquals(1_024, slot.getMaxStackSize(ItemStack(Items.STONE)))
        assertTrue(slot.safeInsert(ItemStack(Items.STONE, 64), 64).isEmpty)
        assertEquals(320, handler.stack.count)
    }

    @Test
    fun `slot extracts at most one normal item stack onto the cursor`() {
        val handler = TestLargeHandler(ItemStack(Items.STONE, 256))
        val slot = LargeItemHandlerSlot(handler, 0)

        assertEquals(64, slot.remove(1_024).count)
        assertEquals(192, handler.stack.count)
    }

    @Test
    fun `large item element retains the regular LDLib item slot identity`() {
        val handler = TestLargeHandler(ItemStack.EMPTY)

        assertEquals("item-slot", LargeItemSlot(LargeItemHandlerSlot(handler, 0)).name())
    }

    private class TestLargeHandler(
        initialStack: ItemStack,
    ) : IItemHandlerModifiable {
        var stack = initialStack
            private set

        override fun getSlots(): Int = 1

        override fun getStackInSlot(slot: Int): ItemStack = stack

        override fun insertItem(
            slot: Int,
            offered: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            if (offered.isEmpty || !isItemValid(slot, offered)) return offered
            val accepted = min(offered.count, getSlotLimit(slot) - stack.count)
            if (!simulate && accepted > 0) {
                stack =
                    if (stack.isEmpty) {
                        offered.copyWithCount(accepted)
                    } else {
                        stack.copyWithCount(stack.count + accepted)
                    }
            }
            return if (accepted == offered.count) ItemStack.EMPTY else offered.copyWithCount(offered.count - accepted)
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            if (stack.isEmpty || amount <= 0) return ItemStack.EMPTY
            val extracted = min(amount, min(stack.count, stack.maxStackSize))
            val result = stack.copyWithCount(extracted)
            if (!simulate) {
                val remaining = stack.count - extracted
                stack = if (remaining == 0) ItemStack.EMPTY else stack.copyWithCount(remaining)
            }
            return result
        }

        override fun getSlotLimit(slot: Int): Int = 1_024

        override fun isItemValid(
            slot: Int,
            offered: ItemStack,
        ): Boolean = stack.isEmpty || ItemStack.isSameItemSameComponents(stack, offered)

        override fun setStackInSlot(
            slot: Int,
            replacement: ItemStack,
        ) {
            stack = replacement.copy()
        }
    }
}
