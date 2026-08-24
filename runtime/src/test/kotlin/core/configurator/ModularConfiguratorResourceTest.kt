package rhx.lazy.core.configurator

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModularConfiguratorResourceTest {
    @Test
    fun `recipe model texture and translations are packaged`() {
        val recipe = readJson("/data/lazy/recipe/modular_configurator.json")
        assertEquals(listOf("IGI", "RCR", "IBI"), recipe["pattern"].asJsonArray.map { it.asString })
        assertEquals("lazy:configuration_card", recipe["key"].asJsonObject["C"].asJsonObject["item"].asString)
        assertEquals("minecraft:chest", recipe["key"].asJsonObject["B"].asJsonObject["item"].asString)
        assertEquals("lazy:modular_configurator", recipe["result"].asJsonObject["id"].asString)

        val model = readJson("/assets/lazy/models/item/modular_configurator.json")
        assertEquals("lazy:item/icon/modular_configurator", model["textures"].asJsonObject["layer0"].asString)
        val image =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/textures/item/icon/modular_configurator.png"))
                .use(ImageIO::read)
        assertEquals(16, image.width)
        assertEquals(16, image.height)

        assertEquals(
            "Modular Configurator",
            readJson("/assets/lazy/lang/en_us.json")["item.lazy.modular_configurator"].asString,
        )
        assertEquals(
            "模块化配置器",
            readJson("/assets/lazy/lang/zh_cn.json")["item.lazy.modular_configurator"].asString,
        )
    }

    @Test
    fun `GuideME guide binds both language pages to the item hotkey`() {
        val guide = readJson("/assets/lazy/guideme_guides/guide.json")
        assertEquals("en_us", guide["default_language"].asString)
        assertEquals(
            "guide.lazy.name",
            guide["item_settings"].asJsonObject["display_name"].asJsonObject["translate"].asString,
        )

        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))
        val english =
            Files.readString(
                projectRoot.resolve("mod/src/main/resources/assets/lazy/guides/lazy/guide/modular_configurator.md"),
            )
        val chinese =
            Files.readString(
                projectRoot.resolve("mod/src/main/resources/assets/lazy/guides/lazy/guide/_zh_cn/modular_configurator.md"),
            )
        listOf(english, chinese).forEach { page ->
            assertTrue(page.contains("item_ids:"))
            assertTrue(page.contains("lazy:modular_configurator"))
        }
        assertNotNull(javaClass.getResource("/assets/lazy/guides/lazy/guide/index.md"))
    }

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
