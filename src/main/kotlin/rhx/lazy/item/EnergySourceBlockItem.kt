package rhx.lazy.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block
import rhx.lazy.util.ENERGY_TRANSFER_LIMIT

internal class EnergySourceBlockItem(
    block: Block,
    properties: Item.Properties,
) : BlockItem(block, properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        tooltipComponents.addEnergyTransferTooltip(ENERGY_TRANSFER_LIMIT)
        tooltipComponents +=
            Component
                .translatable("tooltip.lazy.energy_source.active_push")
                .withStyle(ChatFormatting.GRAY)
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}
