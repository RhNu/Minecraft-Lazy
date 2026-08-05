package rhx.lazy.feature.buffer

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block

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
            val managed = data.copyTag().getCompound(BufferBlockEntity.MANAGED_DATA_KEY)
            val itemTotal = managed.getInt(BufferBlockEntity.ITEM_TOTAL_FIELD)
            val fluidTotal = managed.getInt(BufferBlockEntity.FLUID_TOTAL_FIELD)
            if (itemTotal > 0 || fluidTotal > 0) {
                tooltipComponents +=
                    Component
                        .translatable(
                            "tooltip.lazy.buffer.contents",
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
