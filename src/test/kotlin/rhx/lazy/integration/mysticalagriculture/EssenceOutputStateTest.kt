package rhx.lazy.integration.mysticalagriculture

import kotlin.test.Test
import kotlin.test.assertEquals

class EssenceOutputStateTest {
    @Test
    fun `valid paused network state survives loading`() {
        val state = EssenceOutputState(EssenceOutputMode.NETWORK, networkId = 7, networkPaused = true)

        assertEquals(state, state.repairAfterLoad(networkAvailable = true))
    }

    @Test
    fun `missing network integration falls back to downward output`() {
        val repaired =
            EssenceOutputState(EssenceOutputMode.NETWORK, networkId = 7, networkPaused = true)
                .repairAfterLoad(networkAvailable = false)

        assertEquals(EssenceOutputState.downward(), repaired)
    }

    @Test
    fun `stale network fields are cleared outside network mode`() {
        val repaired =
            EssenceOutputState(EssenceOutputMode.DOWNWARD, networkId = 7, networkPaused = true)
                .repairAfterLoad(networkAvailable = true)

        assertEquals(EssenceOutputState.downward(), repaired)
    }
}
