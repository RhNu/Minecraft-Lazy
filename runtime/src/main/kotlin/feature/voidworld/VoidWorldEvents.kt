package rhx.lazy.feature.voidworld

import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import rhx.lazy.feature.teleporter.TeleporterService

internal object VoidWorldEvents {
    fun onServerStarted(event: ServerStartedEvent) {
        EncapsulatedSpaceService.recover(event.server)
    }

    fun onEntityJoin(event: EntityJoinLevelEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (event.level.dimension() != VoidWorldKeys.voidLevel) return
        VoidHubPlatform.ensureCreated(player.server)
        TeleporterService.rescueFromUnregisteredVoid(player)
    }

    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (player.level().dimension() != VoidWorldKeys.voidLevel) return
        VoidHubPlatform.ensureCreated(player.server)
        TeleporterService.rescueFromUnregisteredVoid(player)
    }
}
