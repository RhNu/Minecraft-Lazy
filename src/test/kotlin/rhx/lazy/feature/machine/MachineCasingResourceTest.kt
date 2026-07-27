package rhx.lazy.feature.machine

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class MachineCasingResourceTest {
    @Test
    fun `machine casing resources are packaged`() {
        val resources =
            listOf(
                "/assets/lazy/blockstates/machine_casing.json",
                "/assets/lazy/models/block/machine_casing.json",
                "/assets/lazy/models/item/machine_casing.json",
                "/data/lazy/loot_table/blocks/machine_casing.json",
                "/data/lazy/recipe/machine_casing.json",
            )

        resources.forEach { path ->
            assertNotNull(javaClass.getResource(path), "Missing generated resource $path")
        }
    }

    @Test
    fun `machine casing has the fixed recipe model and translations`() {
        val recipe = readJson("/data/lazy/recipe/machine_casing.json")
        assertEquals(
            listOf("III", "IRI", "III"),
            recipe["pattern"].asJsonArray.map { it.asString },
        )
        assertEquals("lazy:machine_casing", recipe["result"].asJsonObject["id"].asString)

        val model = readJson("/assets/lazy/models/block/machine_casing.json")
        assertEquals("minecraft:block/cube_all", model["parent"].asString)
        assertEquals("lazy:block/machine_casing", model["textures"].asJsonObject["all"].asString)

        assertEquals(
            "Machine Casing",
            readJson("/assets/lazy/lang/en_us.json")["block.lazy.machine_casing"].asString,
        )
        assertEquals(
            "机器外壳",
            readJson("/assets/lazy/lang/zh_cn.json")["block.lazy.machine_casing"].asString,
        )
    }

    @Test
    fun `machine recipes use casing in the center`() {
        val expectedPatterns =
            mapOf(
                "buffer" to listOf(" C ", "IMI"),
                "energy_source" to listOf(" B ", "GMG"),
                "item_copier" to listOf(" C ", "BMB"),
                "repairer" to listOf(" A ", "CMC"),
                "planter" to listOf(" P ", "HMH", " C "),
            )

        expectedPatterns.forEach { (machine, expectedPattern) ->
            val recipe = readJson("/data/lazy/recipe/$machine.json")
            assertEquals(expectedPattern, recipe["pattern"].asJsonArray.map { it.asString })
            assertEquals(
                "lazy:machine_casing",
                recipe["key"].asJsonObject["M"].asJsonObject["item"].asString,
            )
        }
    }

    @Test
    fun `machines use casing ends and distinct side textures`() {
        val casing = readTexture("machine_casing")
        val casingPixels = casing.pixels()
        val machines = listOf("buffer", "energy_source", "item_copier", "repairer", "planter")

        machines.forEach { machine ->
            val model = readJson("/assets/lazy/models/block/$machine.json")
            assertEquals("minecraft:block/cube_column", model["parent"].asString)
            assertEquals("lazy:block/$machine", model["textures"].asJsonObject["side"].asString)
            assertEquals("lazy:block/machine_casing", model["textures"].asJsonObject["end"].asString)

            val texture = readTexture(machine)
            assertEquals(16, texture.width)
            assertEquals(16, texture.height)
            assertFalse(texture.pixels().contentEquals(casingPixels), "$machine must have a visible overlay")
        }
    }

    private fun readTexture(name: String): BufferedImage =
        requireNotNull(
            javaClass.getResourceAsStream("/assets/lazy/textures/block/$name.png"),
        ).use(ImageIO::read)

    private fun BufferedImage.pixels(): IntArray = getRGB(0, 0, width, height, null, 0, width)

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
