package rhx.lazy.feature.replicator

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block
import rhx.lazy.core.ManagedBlockEntity
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant

internal class ReplicatorBlockItem(
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
            val registries = context.registries()
            val resource =
                registries?.let {
                    ResourceAmount.parse(it, stored.getCompound(ReplicatorBlockEntity.RESOURCE_TAG))
                }
            if (resource != null) {
                tooltipComponents +=
                    Component
                        .translatable(
                            "tooltip.lazy.replicator.resource",
                            resourceName(resource),
                            resource.amount,
                        ).withStyle(ChatFormatting.GRAY)
            }
            val managed = stored.getCompound(ManagedBlockEntity.MANAGED_DATA_KEY)
            val gear =
                ReplicatorGear.fromInterval(
                    managed.getInt(ReplicatorBlockEntity.PUSH_INTERVAL_FIELD),
                )
            tooltipComponents +=
                Component
                    .translatable("tooltip.lazy.replicator.interval", gear.intervalTicks)
                    .withStyle(ChatFormatting.GRAY)
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }

    @Suppress("UNCHECKED_CAST")
    private fun resourceName(amount: ResourceAmount<out ResourceVariant>): Component {
        val kind = amount.kind as ResourceKind<ResourceVariant>
        return kind.variantName(amount.variant)
    }
}
