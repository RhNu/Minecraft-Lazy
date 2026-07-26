package rhx.lazy.integration.botanypots

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanterResourceTest {
    @Test
    fun `recipe has the fixed pattern and Botany Pots condition`() {
        val recipe = readJson("/data/lazy/recipe/planter.json")
        assertEquals(
            listOf("IPI", "HCH", "IRI"),
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
    fun `static model and translations are generated`() {
        val model = readJson("/assets/lazy/models/block/planter.json")
        assertEquals("minecraft:block/cube_all", model["parent"].asString)
        assertEquals("lazy:block/planter", model["textures"].asJsonObject["all"].asString)

        val english = readJson("/assets/lazy/lang/en_us.json")
        val chinese = readJson("/assets/lazy/lang/zh_cn.json")
        assertEquals("Planter", english["block.lazy.planter"].asString)
        assertEquals("种植机", chinese["block.lazy.planter"].asString)
        assertEquals("Repairer", english["block.lazy.repairer"].asString)
        assertEquals("修复器", chinese["block.lazy.repairer"].asString)
    }

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
