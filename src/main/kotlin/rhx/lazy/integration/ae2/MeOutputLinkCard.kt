package rhx.lazy.integration.ae2

import appeng.api.features.IGridLinkableHandler
import appeng.api.ids.AEComponents
import net.minecraft.ChatFormatting
import net.minecraft.core.GlobalPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.io.IoManagedBlockEntity

internal class MeOutputLinkCard(
    properties: Properties,
) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val machine =
            context.level.getBlockEntity(context.clickedPos) as? IoManagedBlockEntity
                ?: return InteractionResult.PASS
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        val player = context.player as? ServerPlayer ?: return InteractionResult.PASS
        val target = context.itemInHand.get(AEComponents.WIRELESS_LINK_TARGET)
        if (target == null) {
            player.displayActionBar("message.lazy.me_output_link_card.unlinked")
            return InteractionResult.FAIL
        }
        if (!machine.ioController.setNetworkTarget(Ae2NetworkOutputProvider.createTarget(target))) {
            player.displayActionBar("message.lazy.me_output_link_card.incompatible")
            return InteractionResult.FAIL
        }
        player.displayActionBar(
            "message.lazy.me_output_link_card.success",
            target.pos.x,
            target.pos.y,
            target.pos.z,
        )
        return InteractionResult.SUCCESS
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        val target = stack.get(AEComponents.WIRELESS_LINK_TARGET)
        if (target == null) {
            tooltipComponents.add(
                Component.translatable("tooltip.lazy.me_output_link_card.unlinked").withStyle(ChatFormatting.GRAY),
            )
        } else {
            tooltipComponents.add(
                Component
                    .translatable(
                        "tooltip.lazy.me_output_link_card.linked",
                        Component.translatable(
                            "dimension.${target.dimension.location().namespace}.${target.dimension.location().path}",
                        ),
                    ).withStyle(ChatFormatting.GRAY),
            )
            tooltipComponents.add(
                Component
                    .translatable(
                        "tooltip.lazy.me_output_link_card.position",
                        target.pos.x,
                        target.pos.y,
                        target.pos.z,
                    ).withStyle(ChatFormatting.DARK_GRAY),
            )
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }

    companion object {
        val LINKABLE_HANDLER: IGridLinkableHandler =
            object : IGridLinkableHandler {
                override fun canLink(stack: ItemStack): Boolean = Ae2Registries.isLinkCard(stack)

                override fun link(
                    itemStack: ItemStack,
                    pos: GlobalPos,
                ) {
                    itemStack.set(AEComponents.WIRELESS_LINK_TARGET, pos)
                }

                override fun unlink(itemStack: ItemStack) {
                    itemStack.remove(AEComponents.WIRELESS_LINK_TARGET)
                }
            }
    }
}
