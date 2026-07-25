package rhx.lazy.teleport

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import rhx.lazy.config.ModConfig
import rhx.lazy.registry.ModDataComponents
import rhx.lazy.registry.ModItems
import rhx.lazy.util.displayActionBar

/**
 * Applies the shared activation policy around the underlying teleport operation.
 *
 * Input adapters such as item use and network payloads are responsible only for deciding when and
 * from which stack this use case is invoked.
 */
internal object TeleporterActivation {
    fun isOnCooldown(player: ServerPlayer): Boolean = player.cooldowns.isOnCooldown(ModItems.teleporter.get())

    fun activate(
        player: ServerPlayer,
        stack: ItemStack,
    ) {
        val teleporter = ModItems.teleporter.get()
        if (stack.item !== teleporter) return

        if (player.cooldowns.isOnCooldown(teleporter)) {
            player.displayActionBar("message.lazy.teleporter.cooldown")
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
                player.cooldowns.addCooldown(teleporter, FAILURE_COOLDOWN_TICKS)
                player.displayActionBar(result.translationKey)
            }

            is TeleporterResult.Success -> {
                stack.set(ModDataComponents.teleporterData.get(), result.newData)
                val cooldownSeconds = ModConfig.teleporter.cooldownSeconds.get()
                if (cooldownSeconds > 0) {
                    player.cooldowns.addCooldown(teleporter, cooldownSeconds * TICKS_PER_SECOND)
                }
                player.displayActionBar("message.lazy.teleporter.success", cooldownSeconds)
            }
        }
    }

    private const val TICKS_PER_SECOND = 20
    private const val FAILURE_COOLDOWN_TICKS = 10
}
