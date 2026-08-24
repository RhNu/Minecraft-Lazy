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
            val stored = data.copyTag()
            val itemTotal = stored.sumStore(BufferBlockEntity.ITEM_STORE_TAG)
            val fluidTotal = stored.sumStore(BufferBlockEntity.FLUID_STORE_TAG)
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

    private fun net.minecraft.nbt.CompoundTag.sumStore(key: String): Long =
        getList(
            key,
            net.minecraft.nbt.Tag.TAG_COMPOUND
                .toInt(),
        ).sumOf { raw ->
            (raw as? net.minecraft.nbt.CompoundTag)?.getLong("amount") ?: 0L
        }
}
