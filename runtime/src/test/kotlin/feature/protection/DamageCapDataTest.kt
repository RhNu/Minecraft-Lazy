package rhx.lazy.feature.protection

import rhx.lazy.core.testing.jsonRoundTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DamageCapDataTest {
    @Test
    fun `codec preserves configured state`() {
        val data = DamageCapData(enabled = true, threshold = 12)
        val decoded = DamageCapData.CODEC.jsonRoundTrip(data)

        assertEquals(data, decoded)
    }

    @Test
    fun `negative thresholds are rejected`() {
        assertTrue(runCatching { DamageCapData(enabled = true, threshold = -1) }.isFailure)
    }
}
