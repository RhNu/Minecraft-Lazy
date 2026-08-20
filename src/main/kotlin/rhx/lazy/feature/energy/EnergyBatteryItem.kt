package rhx.lazy.feature.energy

import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.ItemHandlerHelper

internal class EnergyBatteryItem(
    properties: Properties,
) : Item(properties) {
    override fun isFoil(stack: ItemStack): Boolean = true

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val player = context.player
        if (player == null || !player.isShiftKeyDown) {
            return super.useOn(context)
        }

        val stack = context.itemInHand
        val handler =
            sequenceOf(context.clickedFace)
                .plus(Direction.entries.asSequence().filter { it != context.clickedFace })
                .mapNotNull { face ->
                    level.getCapability(Capabilities.ItemHandler.BLOCK, context.clickedPos, face)
                }.firstOrNull { candidate ->
                    ItemHandlerHelper.insertItemStacked(candidate, stack.copy(), true).count < stack.count
                } ?: return super.useOn(context)
        if (level.isClientSide) return InteractionResult.SUCCESS

        val remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false)
        player.setItemInHand(context.hand, remainder)
        return InteractionResult.SUCCESS
    }
}
