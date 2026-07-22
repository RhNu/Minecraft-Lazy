package rhx.compose.network

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import rhx.compose.MOD_ID
import rhx.compose.menu.BufferMenu

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
internal object ModNetwork {
    private const val NETWORK_VERSION = "1"

    @SubscribeEvent
    fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        event
            .registrar(NETWORK_VERSION)
            .playToClient(
                BufferContentsPayload.TYPE,
                BufferContentsPayload.STREAM_CODEC,
            ) { payload, context ->
                val menu = context.player().containerMenu
                if (menu is BufferMenu && menu.containerId == payload.containerId) {
                    menu.applyClientSnapshot(payload.snapshot)
                }
            }
            .playToServer(
                ClearBufferPayload.TYPE,
                ClearBufferPayload.STREAM_CODEC,
            ) { payload, context ->
                val player = context.player()
                val menu = player.containerMenu
                if (menu is BufferMenu && menu.containerId == payload.containerId) {
                    menu.clearContents(player)
                }
            }
    }
}
