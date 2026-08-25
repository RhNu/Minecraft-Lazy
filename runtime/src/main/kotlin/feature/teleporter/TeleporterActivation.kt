package rhx.lazy.feature.teleporter

import net.minecraft.server.level.ServerPlayer

internal object TeleporterActivation {
    fun isOnCooldown(player: ServerPlayer): Boolean = player.cooldowns.isOnCooldown(TeleporterRegistries.item.get())

    fun isDimensionBlacklisted(player: ServerPlayer): Boolean = TeleporterDimensionBlacklist.contains(player.level().dimension())

    const val DIMENSION_BLACKLISTED: String = "message.lazy.teleporter.dimension_blacklisted"
}
