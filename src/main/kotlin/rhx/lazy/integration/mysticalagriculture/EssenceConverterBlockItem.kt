package rhx.lazy.integration.mysticalagriculture

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block

internal class EssenceConverterBlockItem(
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
            val managed = data.copyTag().getCompound(EssenceConverterBlockEntity.MANAGED_DATA_KEY)
            val tier =
                EssenceTier.fromSerializedName(
                    managed.getString(EssenceConverterBlockEntity.TARGET_TIER_FIELD),
                )
            val count = managed.getLong(EssenceConverterBlockEntity.STORED_OUTPUT_FIELD)
            val remainder = managed.getInt(EssenceConverterBlockEntity.STORED_REMAINDER_FIELD)
            if (tier != null && (count > 0L || remainder > 0)) {
                tooltipComponents +=
                    Component
                        .translatable(
                            "tooltip.lazy.essence_converter.contents",
                            tier.createStack().hoverName,
                            count,
                            remainder,
                        ).withStyle(ChatFormatting.GRAY)
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}
