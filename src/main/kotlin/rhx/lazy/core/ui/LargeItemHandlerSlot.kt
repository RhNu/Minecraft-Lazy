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
    handler: IItemHandlerModifiable,
    index: Int,
) : ItemHandlerSlot(handler, index) {
    override fun getMaxStackSize(stack: ItemStack): Int = maxStackSize

    override fun remove(amount: Int): ItemStack = super.remove(min(amount, item.maxStackSize.coerceAtLeast(1)))
}
