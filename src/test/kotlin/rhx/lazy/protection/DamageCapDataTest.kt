package rhx.lazy.protection

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DamageCapDataTest {
    @Test
    fun `codec preserves configured state`() {
        val data = DamageCapData(enabled = true, threshold = 12)
        val encoded =
            DamageCapData.CODEC
                .encodeStart(JsonOps.INSTANCE, data)
                .result()
                .orElseThrow()
        val decoded =
            DamageCapData.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .result()
                .orElseThrow()

        assertEquals(data, decoded)
    }

    @Test
    fun `negative thresholds are rejected`() {
        val invalid = JsonParser.parseString("""{"enabled":true,"threshold":-1}""")

        assertTrue(DamageCapData.CODEC.parse(JsonOps.INSTANCE, invalid).isError)
        assertTrue(runCatching { DamageCapData(enabled = true, threshold = -1) }.isFailure)
    }
}
