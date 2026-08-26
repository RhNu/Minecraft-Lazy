package rhx.lazy.feature.simulation

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext

internal object SimulationNetworking {
    fun register(event: RegisterPayloadHandlersEvent) {
        event
            .registrar(NETWORK_VERSION)
            .playToClient(
                AutomaticSimulationSnapshotPayload.TYPE,
                AutomaticSimulationSnapshotPayload.STREAM_CODEC,
                ::handleSnapshot,
            )
    }

    private fun handleSnapshot(
        payload: AutomaticSimulationSnapshotPayload,
        context: IPayloadContext,
    ) {
        context.enqueueWork { AutomaticSimulationClientSnapshot.replace(payload.displays) }
    }

    private const val NETWORK_VERSION = "2"
}
