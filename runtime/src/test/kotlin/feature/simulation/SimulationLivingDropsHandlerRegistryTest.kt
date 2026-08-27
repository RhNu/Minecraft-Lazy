package rhx.lazy.feature.simulation

import kotlin.test.Test
import kotlin.test.assertNotNull

class SimulationLivingDropsHandlerRegistryTest {
    @Test
    fun `same handler instance cannot be registered twice`() {
        val registry = SimulationLivingDropsHandlerRegistry()
        val handler = SimulationLivingDropsHandler { }
        registry.register(handler)

        val failure =
            try {
                registry.register(handler)
                null
            } catch (exception: IllegalStateException) {
                exception
            }

        assertNotNull(failure)
    }
}
