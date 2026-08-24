package rhx.lazy.feature.teleporter

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.displayActionBar
import rhx.lazy.integration.api.LazyInternalApi

/**
 * Applies the shared activation policy around the underlying teleport operation.
 *
 * Input adapters such as item use and network payloads are responsible only for deciding when and
 * from which stack this use case is invoked.
 */
@LazyInternalApi
public object TeleporterActivation {
    fun isOnCooldown(player: ServerPlayer): Boolean = player.cooldowns.isOnCooldown(TeleporterRegistries.item.get())

    fun isDimensionBlacklisted(player: ServerPlayer): Boolean = TeleporterDimensionBlacklist.contains(player.level().dimension())

    fun activate(
        player: ServerPlayer,
        stack: ItemStack,
    ) {
        val teleporter = TeleporterRegistries.item.get()
        if (stack.item !== teleporter) return

        if (isDimensionBlacklisted(player)) {
            player.displayActionBar(DIMENSION_BLACKLISTED)
            return
        }

        if (player.cooldowns.isOnCooldown(teleporter)) {
            player.displayActionBar("message.lazy.teleporter.cooldown")
            return
        }

        val service =
            TeleporterService(
                TeleporterSettings(
                    safeSearchRadius = TeleporterConfigs.settings.safeSearchRadius.get(),
                    createVoidSafetyPlatform = TeleporterConfigs.settings.createVoidSafetyPlatform.get(),
                ),
            )
        val oldData = stack.get(TeleporterRegistries.dataComponent.get()) ?: TeleporterData.EMPTY
        when (val result = service.teleport(player, oldData)) {
            is TeleporterResult.Failure -> {
                player.cooldowns.addCooldown(teleporter, FAILURE_COOLDOWN_TICKS)
                player.displayActionBar(result.translationKey)
            }

            is TeleporterResult.Success -> {
                stack.set(TeleporterRegistries.dataComponent.get(), result.newData)
                val cooldownSeconds = TeleporterConfigs.settings.cooldownSeconds.get()
                if (cooldownSeconds > 0) {
                    player.cooldowns.addCooldown(teleporter, cooldownSeconds * TICKS_PER_SECOND)
                }
                player.displayActionBar("message.lazy.teleporter.success", cooldownSeconds)
            }
        }
    }

    private const val TICKS_PER_SECOND = 20
    private const val FAILURE_COOLDOWN_TICKS = 10
    const val DIMENSION_BLACKLISTED = "message.lazy.teleporter.dimension_blacklisted"
}
