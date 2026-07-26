package rhx.lazy.feature.itemcopier

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ItemCopierResourceTest {
    @Test
    fun `generated item copier resources are packaged`() {
        val resources =
            listOf(
                "/assets/lazy/blockstates/item_copier.json",
                "/assets/lazy/models/block/item_copier.json",
                "/assets/lazy/models/item/item_copier.json",
                "/data/lazy/loot_table/blocks/item_copier.json",
                "/data/lazy/recipe/item_copier.json",
            )

        resources.forEach { path ->
            assertNotNull(javaClass.getResource(path), "Missing generated resource $path")
        }
    }

    @Test
    fun `item copier texture is a nonempty sixteen pixel texture`() {
        val image =
            requireNotNull(
                javaClass.getResourceAsStream("/assets/lazy/textures/block/item_copier.png"),
            ).use(ImageIO::read)

        assertEquals(16, image.width)
        assertEquals(16, image.height)
        assertTrue(image.hasMoreThanOneColor())
    }

    private fun BufferedImage.hasMoreThanOneColor(): Boolean {
        val first = getRGB(0, 0)
        return (0 until width).any { x ->
            (0 until height).any { y -> getRGB(x, y) != first }
        }
    }
}
