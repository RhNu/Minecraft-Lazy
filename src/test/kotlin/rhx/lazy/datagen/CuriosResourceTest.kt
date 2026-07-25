package rhx.lazy.datagen

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class CuriosResourceTest {
    @Test
    fun `teleporter slot is available to players and uses its strict validator`() {
        val slot = readJson("/data/lazy/curios/slots/teleporter.json")
        assertEquals(1, slot["size"].asInt)
        assertEquals("curios:slot/empty_curio_slot", slot["icon"].asString)
        assertEquals(listOf("lazy:teleporter_slot"), slot["validators"].asJsonArray.map { it.asString })

        val entities = readJson("/data/lazy/curios/entities/teleporter.json")
        assertEquals(listOf("minecraft:player"), entities["entities"].asJsonArray.map { it.asString })
        assertEquals(listOf("teleporter"), entities["slots"].asJsonArray.map { it.asString })
    }

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
