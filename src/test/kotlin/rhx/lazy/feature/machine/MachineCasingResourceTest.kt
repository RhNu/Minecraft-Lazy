package rhx.lazy.feature.machine

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun `machines use casing tops and bottoms and distinct side textures`() {
        val casing = readTexture("machine_casing")
        val casingPixels = casing.pixels()
        val columnMachines = listOf("buffer", "energy_source", "item_copier", "repairer")

        columnMachines.forEach { machine ->
            val model = readJson("/assets/lazy/models/block/$machine.json")
            assertEquals("minecraft:block/cube_bottom_top", model["parent"].asString)
            assertEquals("lazy:block/$machine", model["textures"].asJsonObject["side"].asString)
            assertEquals("lazy:block/machine_casing", model["textures"].asJsonObject["bottom"].asString)
            assertEquals("lazy:block/machine_casing", model["textures"].asJsonObject["top"].asString)

            val texture = readTexture(machine)
            assertEquals(16, texture.width)
            assertEquals(16, texture.height)
            assertFalse(texture.pixels().contentEquals(casingPixels), "$machine must have a visible overlay")
        }

        val planterModel = readJson("/assets/lazy/models/block/planter.json")
        assertEquals("minecraft:block/cube_bottom_top", planterModel["parent"].asString)
        assertEquals("lazy:block/planter", planterModel["textures"].asJsonObject["side"].asString)
        assertEquals("lazy:block/machine_casing", planterModel["textures"].asJsonObject["bottom"].asString)
        assertEquals("lazy:block/planter_top", planterModel["textures"].asJsonObject["top"].asString)

        val planterTexture = readTexture("planter")
        assertEquals(16, planterTexture.width)
        assertEquals(16, planterTexture.height)
        assertFalse(planterTexture.pixels().contentEquals(casingPixels), "planter must have a visible overlay")

        val essenceConverterModel = readJson("/assets/lazy/models/block/essence_converter.json")
        assertEquals("minecraft:block/cube_bottom_top", essenceConverterModel["parent"].asString)
        assertEquals(
            "lazy:block/essence_converter",
            essenceConverterModel["textures"].asJsonObject["side"].asString,
        )
        assertEquals(
            "lazy:block/machine_casing",
            essenceConverterModel["textures"].asJsonObject["bottom"].asString,
        )
        assertEquals(
            "lazy:block/machine_casing",
            essenceConverterModel["textures"].asJsonObject["top"].asString,
        )
    }

    @Test
    fun `machine textures keep opposite casing rails equally bright`() {
        val machineTextures =
            listOf(
                "machine_casing",
                "buffer",
                "energy_source",
                "item_copier",
                "repairer",
                "planter",
                "planter_top",
            )

        machineTextures.forEach { name ->
            val texture = readTexture(name)
            assertEquals(texture.getRGB(2, 0), texture.getRGB(2, 15), "$name top and bottom outer rails")
            assertEquals(texture.getRGB(0, 2), texture.getRGB(15, 2), "$name left and right outer rails")
            assertEquals(texture.getRGB(2, 1), texture.getRGB(2, 14), "$name top and bottom frame rails")
            assertEquals(texture.getRGB(1, 2), texture.getRGB(14, 2), "$name left and right frame rails")
        }
    }

    @Test
    fun `every machine texture has an svg source`() {
        val artRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")), "art", "block")
        val sources =
            listOf(
                "machine_casing_base.svg",
                "buffer_overlay.svg",
                "energy_source_overlay.svg",
                "item_copier_overlay.svg",
                "repairer_overlay.svg",
                "planter_overlay.svg",
                "planter_top_overlay.svg",
            )

        sources.forEach { source ->
            assertTrue(Files.isRegularFile(artRoot.resolve(source)), "Missing SVG source art/block/$source")
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
