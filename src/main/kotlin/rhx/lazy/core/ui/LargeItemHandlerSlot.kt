package rhx.lazy.core.ui

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandlerModifiable
import kotlin.math.min

/**
 * Adapts an oversized NeoForge item-handler slot to vanilla container interactions.
 *
 * Vanilla normally caps insertion at the item's ordinary stack size. This slot instead exposes the
 * handler's capacity while ensuring extraction never puts an oversized stack on the cursor, in a
 * player inventory, or into the world.
 */
internal class LargeItemHandlerSlot(
    private val handler: IItemHandlerModifiable,
    private val handlerSlot: Int,
) : ItemHandlerSlot(handler, handlerSlot) {
    override fun getMaxStackSize(stack: ItemStack): Int = maxStackSize

    override fun remove(amount: Int): ItemStack = super.remove(min(amount, item.maxStackSize.coerceAtLeast(1)))

    /** The visible stack is capped at 64, so vanilla cannot derive the real remaining capacity. */
    override fun safeInsert(
        stack: ItemStack,
        increment: Int,
    ): ItemStack {
        if (stack.isEmpty || increment <= 0 || !mayPlace(stack)) return stack
        val offered = min(increment, stack.count)
        val remainder = handler.insertItem(handlerSlot, stack.copyWithCount(offered), false)
        val inserted = offered - remainder.count.coerceIn(0, offered)
        if (inserted > 0) stack.shrink(inserted)
        return stack
    }

    /**
     * LDLib quick-move edits a copy returned by the handler and writes the remainder back instead of
     * calling [remove]. Replacing a long-count slot with that at-most-one-stack remainder would either
     * duplicate or discard everything beyond the visible window, so reconcile only the visible delta.
     */
    override fun setByPlayer(
        stack: ItemStack,
        previous: ItemStack,
    ) {
        when {
            previous.isEmpty && stack.isEmpty -> return
            previous.isEmpty -> insert(stack.count, stack)
            stack.isEmpty -> extract(previous.count)
            ItemStack.isSameItemSameComponents(previous, stack) -> {
                val delta = stack.count - previous.count
                if (delta > 0) {
                    insert(delta, stack)
                } else if (delta < 0) {
                    extract(-delta)
                }
            }
            else -> super.setByPlayer(stack, previous)
        }
        setChanged()
    }

    private fun insert(
        amount: Int,
        template: ItemStack,
    ) {
        if (amount > 0) handler.insertItem(handlerSlot, template.copyWithCount(amount), false)
    }

    private fun extract(amount: Int) {
        if (amount > 0) handler.extractItem(handlerSlot, amount, false)
    }
}
