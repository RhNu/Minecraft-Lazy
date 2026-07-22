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
        if (serverPlayer.cooldowns.isOnCooldown(this)) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.lazy.teleporter.cooldown"),
                true,
            )
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
        if (player.cooldowns.isOnCooldown(this)) return

        val usedTicks = getUseDuration(stack, entity) - timeLeft
        if (usedTicks < ModConfig.teleporter.chargeTicks.get()) {
            player.displayClientMessage(
                Component.translatable("message.lazy.teleporter.charge_not_full"),
                true,
            )
            return
        }

        val service =
            TeleporterService(
                TeleporterSettings(
                    safeSearchRadius = ModConfig.teleporter.safeSearchRadius.get(),
                    createVoidSafetyPlatform = ModConfig.teleporter.createVoidSafetyPlatform.get(),
                ),
            )
        val oldData = stack.get(ModDataComponents.teleporterData.get()) ?: TeleporterData.EMPTY
        when (val result = service.teleport(player, oldData)) {
            is TeleporterResult.Failure -> {
                player.cooldowns.addCooldown(this, FAILURE_COOLDOWN_TICKS)
                player.displayClientMessage(Component.translatable(result.translationKey), true)
            }

            is TeleporterResult.Success -> {
                stack.set(ModDataComponents.teleporterData.get(), result.newData)
                val cooldownSeconds = ModConfig.teleporter.cooldownSeconds.get()
                if (cooldownSeconds > 0) {
                    player.cooldowns.addCooldown(this, cooldownSeconds * TICKS_PER_SECOND)
                }
                player.displayClientMessage(
                    Component.translatable("message.lazy.teleporter.success", cooldownSeconds),
                    true,
                )
            }
        }
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
        const val TICKS_PER_SECOND = 20
        const val FAILURE_COOLDOWN_TICKS = 10
        const val MAX_USE_TICKS = 72_001
    }
}
