package rhx.lazy.item

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import rhx.lazy.config.ModConfig
import rhx.lazy.registry.ModDataComponents
import rhx.lazy.teleport.SavedLocation
import rhx.lazy.teleport.TeleporterActivation
import rhx.lazy.teleport.TeleporterData
import rhx.lazy.util.displayActionBar

internal class TeleporterItem(
    properties: Properties,
) : Item(properties) {
    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand,
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (level.isClientSide) {
            player.startUsingItem(usedHand)
            return InteractionResultHolder.consume(stack)
        }

        val serverPlayer = player as? ServerPlayer ?: return InteractionResultHolder.fail(stack)
        if (TeleporterActivation.isOnCooldown(serverPlayer)) {
            serverPlayer.displayActionBar("message.lazy.teleporter.cooldown")
            return InteractionResultHolder.fail(stack)
        }

        serverPlayer.startUsingItem(usedHand)
        return InteractionResultHolder.consume(stack)
    }

    override fun releaseUsing(
        stack: ItemStack,
        level: Level,
        entity: LivingEntity,
        timeLeft: Int,
    ) {
        if (level.isClientSide) return
        val player = entity as? ServerPlayer ?: return
        if (TeleporterActivation.isOnCooldown(player)) return

        val usedTicks = getUseDuration(stack, entity) - timeLeft
        if (usedTicks < ModConfig.teleporter.chargeTicks.get()) {
            player.displayActionBar("message.lazy.teleporter.charge_not_full")
            return
        }

        TeleporterActivation.activate(player, stack)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.BOW

    override fun getUseDuration(
        stack: ItemStack,
        entity: LivingEntity,
    ): Int = MAX_USE_TICKS

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        val data = stack.get(ModDataComponents.teleporterData.get()) ?: TeleporterData.EMPTY
        data.returnLocation?.let { location ->
            tooltipComponents.add(location.tooltip("tooltip.lazy.teleporter.return"))
        }
        data.targetLocation?.let { location ->
            tooltipComponents.add(location.tooltip("tooltip.lazy.teleporter.target"))
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }

    private fun SavedLocation.tooltip(translationKey: String): Component =
        Component.translatable(
            translationKey,
            Component.translatable("dimension.${dimension.location().namespace}.${dimension.location().path}"),
            pos.x,
            pos.y,
            pos.z,
        )

    private companion object {
        const val MAX_USE_TICKS = 72_001
    }
}
