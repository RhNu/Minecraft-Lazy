package rhx.lazy.feature.simulation

import kotlin.test.Test
import kotlin.test.assertEquals

class SimulationRollBudgetTest {
    @Test
    fun `budget is independent of output mode`() {
        assertEquals(16, simulationRollBudget(432, 16))
        assertEquals(4096, simulationRollBudget(65_536, 4096))
    }

    @Test
    fun `remaining work caps the configured budget`() {
        assertEquals(7, simulationRollBudget(7, 16))
    }

    @Test
    fun `malformed large work still observes the configured budget`() {
        assertEquals(16, simulationRollBudget(Long.MAX_VALUE, 16))
    }
}
