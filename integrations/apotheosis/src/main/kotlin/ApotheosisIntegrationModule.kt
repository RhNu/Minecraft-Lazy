package rhx.lazy.integration.apotheosis

import rhx.lazy.feature.simulation.SimulationIncinerationHandlers
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object ApotheosisIntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        SimulationIncinerationHandlers.register(ApotheosisIncinerationHandler)
    }
}
