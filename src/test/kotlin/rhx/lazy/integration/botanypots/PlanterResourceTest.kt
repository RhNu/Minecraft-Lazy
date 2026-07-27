package rhx.lazy.integration.botanypots

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.Color
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanterResourceTest {
    @Test
    fun `recipe has the fixed pattern and Botany Pots condition`() {
        val recipe = readJson("/data/lazy/recipe/planter.json")
        assertEquals(
            listOf(" P ", "HMH", " C "),
            recipe["pattern"].asJsonArray.map { it.asString },
        )
        assertEquals("lazy:planter", recipe["result"].asJsonObject["id"].asString)

        val condition = recipe["neoforge:conditions"].asJsonArray.single().asJsonObject
        assertEquals("neoforge:mod_loaded", condition["type"].asString)
        assertEquals("botanypots", condition["modid"].asString)
    }

    @Test
    fun `loot table is only loaded with Botany Pots`() {
        val lootTable = readJson("/data/lazy/loot_table/blocks/planter.json")
        val condition = lootTable["neoforge:conditions"].asJsonArray.single().asJsonObject
        assertEquals("neoforge:mod_loaded", condition["type"].asString)
        assertEquals("botanypots", condition["modid"].asString)
    }

    @Test
    fun `directional model and translations are generated`() {
        val model = readJson("/assets/lazy/models/block/planter.json")
        assertEquals("minecraft:block/cube_bottom_top", model["parent"].asString)
        assertEquals("lazy:block/planter", model["textures"].asJsonObject["side"].asString)
        assertEquals("lazy:block/machine_casing", model["textures"].asJsonObject["bottom"].asString)
        assertEquals("lazy:block/planter_top", model["textures"].asJsonObject["top"].asString)

        val variants = readJson("/assets/lazy/blockstates/planter.json")["variants"].asJsonObject
        assertEquals(
            setOf("facing=north", "facing=east", "facing=south", "facing=west"),
            variants.keySet(),
        )
        assertEquals("lazy:block/planter", variants["facing=north"].asJsonObject["model"].asString)
        assertEquals(90, variants["facing=east"].asJsonObject["y"].asInt)
        assertEquals(180, variants["facing=south"].asJsonObject["y"].asInt)
        assertEquals(270, variants["facing=west"].asJsonObject["y"].asInt)

        val itemModel = readJson("/assets/lazy/models/item/planter.json")
        assertEquals("lazy:block/planter", itemModel["parent"].asString)

        val english = readJson("/assets/lazy/lang/en_us.json")
        val chinese = readJson("/assets/lazy/lang/zh_cn.json")
        assertEquals("Planter", english["block.lazy.planter"].asString)
        assertEquals("种植机", chinese["block.lazy.planter"].asString)
        assertEquals("Repairer", english["block.lazy.repairer"].asString)
        assertEquals("修复器", chinese["block.lazy.repairer"].asString)
    }

    @Test
    fun `top texture has a framed directional marker`() {
        val image =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/textures/block/planter_top.png"))
                .use(ImageIO::read)
        assertEquals(16, image.width)
        assertEquals(16, image.height)

        val marker = Color(image.getRGB(7, 2), true)
        assertTrue(marker.green > marker.red)
        assertTrue(marker.green > marker.blue)
    }

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
