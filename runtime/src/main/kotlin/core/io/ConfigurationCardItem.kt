package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import rhx.lazy.core.displayActionBar
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class ConfigurationCardItem(
    properties: Properties,
) : Item(properties),
    HeldItemUIMenuType.HeldItemUI {
    /**
     * Right-clicking a machine applies the card to it; sneaking copies the machine's configuration
     * back onto the card. Anything else falls through to [use], which opens the card's own panel.
     */
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val machine = level.getBlockEntity(context.clickedPos) as? IoManagedBlockEntity ?: return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS
        val player = context.player as? ServerPlayer ?: return InteractionResult.PASS
        val stack = context.itemInHand

        if (player.isShiftKeyDown) {
            ConfigurationCardData.set(stack, machine.storedIoConfiguration().deepCopy())
            player.displayActionBar("message.lazy.configuration_card.copied")
            return InteractionResult.SUCCESS
        }

        val configuration = ConfigurationCardData.get(stack)
        if (!machine.ioController.applyConfiguration(configuration)) {
            player.displayActionBar("message.lazy.configuration_card.incompatible")
            return InteractionResult.FAIL
        }
        player.displayActionBar("message.lazy.configuration_card.applied", configuration.mode.translation())
        return InteractionResult.SUCCESS
    }

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
        when (configuration.mode) {
            IoMode.FACE ->
                tooltipComponents +=
                    Component
                        .translatable(
                            "tooltip.lazy.configuration_card.eject",
                            Component.translatable("gui.lazy.io.${if (configuration.autoEject) "enabled" else "disabled"}"),
                        ).withStyle(ChatFormatting.DARK_GRAY)
            IoMode.NETWORK ->
                tooltipComponents +=
                    Component
                        .translatable("tooltip.lazy.configuration_card.network", configuration.networkTargetName())
                        .withStyle(ChatFormatting.DARK_GRAY)
            IoMode.PASSIVE -> Unit
        }
        tooltipComponents += Component.translatable("tooltip.lazy.configuration_card.usage").withStyle(ChatFormatting.DARK_GRAY)
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}

internal fun IoMode.translation(): Component = Component.translatable("gui.lazy.io.mode.${name.lowercase()}")

/** Provider display name for the bound target, or an "unbound" placeholder. */
internal fun IoConfiguration.networkTargetName(): Component {
    val target = networkTarget ?: return Component.translatable("gui.lazy.io.network.unbound")
    return NetworkOutputProviders.get(target.providerId)?.displayName
        ?: Component.translatable("gui.lazy.io.network.unknown_provider", target.providerId.toString())
}
