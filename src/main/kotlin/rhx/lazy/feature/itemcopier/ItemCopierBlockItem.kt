package rhx.lazy.feature.itemcopier

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block

internal class ItemCopierBlockItem(
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
            val managed = data.copyTag().getCompound(ItemCopierBlockEntity.MANAGED_DATA_KEY)
            val registries = context.registries()
            val template =
                if (registries == null) {
                    ItemStack.EMPTY
                } else {
                    ItemStack.parseOptional(
                        registries,
                        managed.getCompound(ItemCopierBlockEntity.TEMPLATE_FIELD),
                    )
                }
            if (!template.isEmpty) {
                tooltipComponents +=
                    Component
                        .translatable("tooltip.lazy.item_copier.template", template.hoverName)
                        .withStyle(ChatFormatting.GRAY)
            }
            val gear =
                ItemCopierGear.fromInterval(
                    managed.getInt(ItemCopierBlockEntity.PUSH_INTERVAL_FIELD),
                )
            tooltipComponents +=
                Component
                    .translatable("tooltip.lazy.item_copier.interval", gear.intervalTicks)
                    .withStyle(ChatFormatting.GRAY)
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}
