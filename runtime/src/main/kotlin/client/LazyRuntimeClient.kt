package rhx.lazy.client

import rhx.lazy.feature.simulation.client.SimulationClientRenderers
import rhx.lazy.integration.api.IntegrationClientContext
import rhx.lazy.integration.api.LazyInternalApi

/** Client-only half of the core runtime bootstrap. */
@LazyInternalApi
public object LazyRuntimeClient {
    public fun install(context: IntegrationClientContext) {
        context.modBus.addListener(SimulationClientRenderers::register)
    }
}
