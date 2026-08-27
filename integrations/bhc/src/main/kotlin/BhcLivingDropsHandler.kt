package rhx.lazy.integration.bhc

import com.traverse.bhc.common.util.DropHandler
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import rhx.lazy.feature.simulation.SimulationLivingDropsHandler

internal object BhcLivingDropsHandler : SimulationLivingDropsHandler {
    override fun onLivingDrops(event: LivingDropsEvent) {
        DropHandler.onEntityDrop(event)
    }
}
