package rhx.compose.item

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block
import rhx.compose.block.entity.BufferBlockEntity

internal class BufferBlockItem(
    block: Block,
    properties: Item.Properties,
) : BlockItem(block, properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        val data = stack.get(DataComponents.BLOCK_ENTITY_DATA)
        if (data != null && !data.isEmpty) {
            val tag = data.copyTag()
            val itemTotal = tag.getInt(BufferBlockEntity.ITEM_TOTAL_KEY)
            val fluidTotal = tag.getInt(BufferBlockEntity.FLUID_TOTAL_KEY)
            if (itemTotal > 0 || fluidTotal > 0) {
                tooltipComponents +=
                    Component
                        .translatable(
                            "tooltip.compose.buffer.contents",
                            itemTotal,
                            BufferBlockEntity.TOTAL_ITEM_CAPACITY,
                            fluidTotal,
                            BufferBlockEntity.TOTAL_FLUID_CAPACITY,
                        ).withStyle(ChatFormatting.GRAY)
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}
