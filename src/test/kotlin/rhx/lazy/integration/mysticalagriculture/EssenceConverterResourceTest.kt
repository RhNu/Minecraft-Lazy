package rhx.lazy.integration.mysticalagriculture

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EssenceConverterResourceTest {
    @Test
    fun `recipe uses the consuming serializer and Agriculture condition`() {
        val recipe = readJson("/data/lazy/recipe/essence_converter.json")

        assertEquals("lazy:consuming_shaped", recipe["type"].asString)
        assertEquals(listOf(" I ", "HMH", " C "), recipe["pattern"].asJsonArray.map { it.asString })
        assertEquals(
            "mysticalagriculture:master_infusion_crystal",
            recipe["key"].asJsonObject["I"].asJsonObject["item"].asString,
        )
        assertEquals("lazy:essence_converter", recipe["result"].asJsonObject["id"].asString)

        val condition = recipe["neoforge:conditions"].asJsonArray.single().asJsonObject
        assertEquals("neoforge:mod_loaded", condition["type"].asString)
        assertEquals("mysticalagriculture", condition["modid"].asString)
    }

    @Test
    fun `loot model texture and translations are generated`() {
        val loot = readJson("/data/lazy/loot_table/blocks/essence_converter.json")
        val condition = loot["neoforge:conditions"].asJsonArray.single().asJsonObject
        assertEquals("mysticalagriculture", condition["modid"].asString)
        assertFalse(loot.has("pools"))

        val model = readJson("/assets/lazy/models/block/essence_converter.json")
        assertEquals("lazy:block/machine_orientable", model["parent"].asString)
        assertEquals(
            "lazy:block/machine/side",
            model["textures"].asJsonObject["side"].asString,
        )
        assertEquals(
            "lazy:block/machine/bottom",
            model["textures"].asJsonObject["bottom"].asString,
        )
        assertEquals(
            "lazy:block/machine/top",
            model["textures"].asJsonObject["top"].asString,
        )
        assertEquals(
            "lazy:block/overlay/essence_converter",
            model["textures"].asJsonObject["overlay"].asString,
        )
        val itemModel = readJson("/assets/lazy/models/item/essence_converter.json")
        assertEquals("lazy:block/essence_converter", itemModel["parent"].asString)

        val image =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/textures/block/overlay/essence_converter.png"))
                .use(ImageIO::read)
        assertEquals(16, image.width)
        assertEquals(16, image.height)

        val english = readJson("/assets/lazy/lang/en_us.json")
        val chinese = readJson("/assets/lazy/lang/zh_cn.json")
        assertEquals("Essence Converter", english["block.lazy.essence_converter"].asString)
        assertEquals("精华转换器", chinese["block.lazy.essence_converter"].asString)
        assertEquals("Target: %s", english["jade.lazy.essence_converter.target"].asString)
        assertEquals("目标：%s", chinese["jade.lazy.essence_converter.target"].asString)
        assertEquals("Essence amount: %s", english["jade.lazy.essence_converter.output"].asString)
        assertEquals("精华量：%s", chinese["jade.lazy.essence_converter.output"].asString)
        assertEquals("Network (paused)", english["gui.lazy.essence_converter.network_paused"].asString)
        assertEquals("网络（已暂停）", chinese["gui.lazy.essence_converter.network_paused"].asString)
        assertFalse(chinese.entrySet().any { (_, value) -> value.asString.contains("Inferium 单位") })
    }

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
