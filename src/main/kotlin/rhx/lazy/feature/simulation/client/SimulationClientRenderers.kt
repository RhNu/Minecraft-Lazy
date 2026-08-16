package rhx.lazy.feature.simulation.client

import net.neoforged.neoforge.client.event.EntityRenderersEvent
import rhx.lazy.core.render.client.MachineDisplayRenderer
import rhx.lazy.feature.simulation.SimulationChamberBlockEntity
import rhx.lazy.feature.simulation.SimulationRegistries

/** Client-side renderers owned by the simulation slice. */
internal object SimulationClientRenderers {
    fun register(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(SimulationRegistries.blockEntity.get()) { context ->
            MachineDisplayRenderer<SimulationChamberBlockEntity>(context)
        }
    }
}
