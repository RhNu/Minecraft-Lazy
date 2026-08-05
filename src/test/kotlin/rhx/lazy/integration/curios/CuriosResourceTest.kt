package rhx.lazy.integration.curios

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CuriosResourceTest {
    @Test
    fun `teleporter slot is available to players and uses its strict validator`() {
        val slot = readJson("/data/lazy/curios/slots/teleporter.json")
        assertEquals(1, slot["size"].asInt)
        assertEquals("lazy:slot/empty/empty_teleporter_slot", slot["icon"].asString)
        assertEquals(listOf("lazy:teleporter_slot"), slot["validators"].asJsonArray.map { it.asString })

        val entities = readJson("/data/lazy/curios/entities/teleporter.json")
        assertEquals(listOf("minecraft:player"), entities["entities"].asJsonArray.map { it.asString })
        assertEquals(listOf("teleporter"), entities["slots"].asJsonArray.map { it.asString })
    }

    @Test
    fun `me link card slot is available to players and uses its strict validator`() {
        val slot = readJson("/data/lazy/curios/slots/me_link_card.json")
        assertEquals(1, slot["size"].asInt)
        assertEquals("lazy:slot/empty/empty_me_link_card_slot", slot["icon"].asString)
        assertEquals(listOf("lazy:me_link_card_slot"), slot["validators"].asJsonArray.map { it.asString })

        val entities = readJson("/data/lazy/curios/entities/me_link_card.json")
        assertEquals(listOf("minecraft:player"), entities["entities"].asJsonArray.map { it.asString })
        assertEquals(listOf("me_link_card"), entities["slots"].asJsonArray.map { it.asString })
    }

    @Test
    fun `me link card slot texture is packaged and has an editable SVG source`() {
        assertNotNull(javaClass.getResourceAsStream("/assets/lazy/textures/slot/empty/empty_me_link_card_slot.png"))

        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))
        assertTrue(Files.isRegularFile(projectRoot.resolve("art/slot/empty/empty_me_link_card_slot.svg")))
    }

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
