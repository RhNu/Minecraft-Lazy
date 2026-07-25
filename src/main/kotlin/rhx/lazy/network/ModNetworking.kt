package rhx.lazy.network

import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import rhx.lazy.curios.CuriosTeleporterLocator
import rhx.lazy.teleport.TeleporterActivation
import java.util.WeakHashMap

internal object ModNetworking {
    fun register(event: RegisterPayloadHandlersEvent) {
        event
            .registrar(NETWORK_VERSION)
            .playToServer(
                ActivateTeleporterPayload.type,
                ActivateTeleporterPayload.streamCodec,
                ::handleActivateTeleporter,
            )
    }

    private fun handleActivateTeleporter(
        payload: ActivateTeleporterPayload,
        context: IPayloadContext,
    ) {
        val player = context.player() as? ServerPlayer ?: return
        val stack = CuriosTeleporterLocator.findEquipped(player) ?: return
        if (player.isChangingDimension || !requestLimiter.tryAcquire(player, player.tickCount)) return

        TeleporterActivation.activate(player, stack)
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
