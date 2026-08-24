package rhx.lazy.feature.shaping

import com.google.gson.JsonParser
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShaperResourceTest {
    @Test
    fun `generated shaper resources and material forms are packaged`() {
        val resources =
            listOf(
                "/assets/lazy/blockstates/shaper.json",
                "/assets/lazy/models/block/shaper.json",
                "/assets/lazy/models/item/shaper.json",
                "/data/lazy/loot_table/blocks/shaper.json",
                "/data/lazy/recipe/shaper.json",
                "/data/lazy/lazy/material_form/ingot.json",
            )

        resources.forEach { path -> assertNotNull(javaClass.getResource(path), "Missing generated resource $path") }
    }

    @Test
    fun `shaper recipe and translations match the implemented machine`() {
        val recipe =
            requireNotNull(javaClass.getResourceAsStream("/data/lazy/recipe/shaper.json"))
                .bufferedReader()
                .use { JsonParser.parseReader(it).asJsonObject }
        assertEquals(listOf(" S ", "CMC"), recipe["pattern"].asJsonArray.map { it.asString })
        assertEquals("lazy:shaper", recipe["result"].asJsonObject["id"].asString)

        val english = language("en_us")
        val chinese = language("zh_cn")
        assertEquals("Shaper", english["block.lazy.shaper"].asString)
        assertEquals("塑形机", chinese["block.lazy.shaper"].asString)
    }

    @Test
    fun `shaper overlay is a nonempty sixteen pixel texture`() {
        val image: BufferedImage =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/textures/block/overlay/shaper.png"))
                .use(ImageIO::read)

        assertEquals(16, image.width)
        assertEquals(16, image.height)
        val first = image.getRGB(0, 0)
        assertTrue((0 until image.width).any { x -> (0 until image.height).any { y -> image.getRGB(x, y) != first } })
    }

    private fun language(locale: String) =
        requireNotNull(javaClass.getResourceAsStream("/assets/lazy/lang/$locale.json"))
            .bufferedReader()
            .use { JsonParser.parseReader(it).asJsonObject }
}
