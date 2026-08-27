package rhx.lazy.integration.bhc

import rhx.lazy.feature.simulation.SimulationLivingDropsHandlers
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object BhcIntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        SimulationLivingDropsHandlers.register(BhcLivingDropsHandler)
    }
}
