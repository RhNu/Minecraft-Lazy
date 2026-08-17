package rhx.lazy.feature.simulation

import rhx.lazy.core.io.IoMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SimulationRollBudgetTest {
    @Test
    fun `network mode settles a full valid batch in one tick`() {
        assertEquals(432, simulationRollBudget(432, 16, IoMode.NETWORK))
        assertEquals(65_536, simulationRollBudget(65_536, 16, IoMode.NETWORK))
    }

    @Test
    fun `non-network modes retain configured roll throttling`() {
        assertEquals(16, simulationRollBudget(432, 16, IoMode.PASSIVE))
        assertEquals(16, simulationRollBudget(432, 16, IoMode.FACE))
    }

    @Test
    fun `network mode bounds malformed legacy batches`() {
        assertEquals(65_536, simulationRollBudget(Long.MAX_VALUE, 16, IoMode.NETWORK))
    }
}
