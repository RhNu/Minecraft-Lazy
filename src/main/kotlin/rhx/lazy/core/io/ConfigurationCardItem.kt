package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

internal class ConfigurationCardItem(
    properties: Properties,
) : Item(properties),
    HeldItemUIMenuType.HeldItemUI {
    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand,
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (!level.isClientSide) {
            val serverPlayer = player as? ServerPlayer ?: return InteractionResultHolder.fail(stack)
            if (!HeldItemUIMenuType.openUI(serverPlayer, usedHand)) return InteractionResultHolder.fail(stack)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun createUI(holder: HeldItemUIMenuType.HeldItemUIHolder): ModularUI = ConfigurationCardUI.create(holder)

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        val configuration = ConfigurationCardData.get(stack)
        tooltipComponents +=
            Component
                .translatable("tooltip.lazy.configuration_card.mode", configuration.mode.translation())
                .withStyle(ChatFormatting.GRAY)
        if (configuration.mode == IoMode.FACE) {
            tooltipComponents +=
                Component
                    .translatable(
                        "tooltip.lazy.configuration_card.eject",
                        Component.translatable("gui.lazy.io.${if (configuration.autoEject) "enabled" else "disabled"}"),
                    ).withStyle(ChatFormatting.DARK_GRAY)
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}

internal fun IoMode.translation(): Component = Component.translatable("gui.lazy.io.mode.${name.lowercase()}")
