package rhx.lazy.integration.curios

import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import rhx.lazy.feature.teleporter.TeleporterUI
import java.util.WeakHashMap

internal object CuriosTeleporterNetworking {
    fun register(event: RegisterPayloadHandlersEvent) {
        event
            .registrar(NETWORK_VERSION)
            .optional()
            .playToServer(
                ActivateEquippedTeleporterPayload.type,
                ActivateEquippedTeleporterPayload.streamCodec,
                ::handleActivateTeleporter,
            )
    }

    private fun handleActivateTeleporter(
        payload: ActivateEquippedTeleporterPayload,
        context: IPayloadContext,
    ) {
        val player = context.player() as? ServerPlayer ?: return
        when (CuriosTeleporterLocator.findEquipped(player)) {
            is EquippedTeleporterResult.Found -> Unit
            EquippedTeleporterResult.NotFound,
            EquippedTeleporterResult.Failed,
            -> return
        }
        if (player.isChangingDimension || !requestLimiter.tryAcquire(player, player.tickCount)) return

        TeleporterUI.open(player)
    }

    private val requestLimiter = SameTickRequestLimiter<ServerPlayer>()
    private const val NETWORK_VERSION = "1"
}

internal class SameTickRequestLimiter<K : Any> {
    private val lastRequestTicks = WeakHashMap<K, Int>()

    fun tryAcquire(
        key: K,
        currentTick: Int,
    ): Boolean {
        if (lastRequestTicks[key] == currentTick) return false

        lastRequestTicks[key] = currentTick
        return true
    }
}
