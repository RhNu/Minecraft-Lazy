package rhx.lazy.core.guide

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class GuideResourceTest {
    @Test
    fun `every machine guide page has bilingual item binding and model header`() {
        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))
        val guideRoot = projectRoot.resolve("mod/src/main/resources/assets/lazy/guides/lazy/guide")
        val indexEnglish = Files.readString(guideRoot.resolve("index.md"))
        val indexChinese = Files.readString(guideRoot.resolve("_zh_cn/index.md"))

        machinePages.forEach { page ->
            val english = Files.readString(guideRoot.resolve(page.fileName))
            val chinese = Files.readString(guideRoot.resolve("_zh_cn").resolve(page.fileName))
            listOf(english, chinese).forEach { content ->
                assertTrue(content.contains("item_ids:"), page.fileName)
                assertTrue(content.contains("- ${page.itemId}"), page.fileName)
                assertTrue(content.contains("<BlockImage id=\"${page.itemId}\" scale=\"8\" />"), page.fileName)
            }
            assertTrue(indexEnglish.contains("(${page.fileName})"), page.fileName)
            assertTrue(indexChinese.contains("(${page.fileName})"), page.fileName)
        }
    }

    @Test
    fun `tool guide pages have bilingual item binding and item header`() {
        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))
        val guideRoot = projectRoot.resolve("mod/src/main/resources/assets/lazy/guides/lazy/guide")
        val indexEnglish = Files.readString(guideRoot.resolve("index.md"))
        val indexChinese = Files.readString(guideRoot.resolve("_zh_cn/index.md"))

        toolPages.forEach { page ->
            val english = Files.readString(guideRoot.resolve(page.fileName))
            val chinese = Files.readString(guideRoot.resolve("_zh_cn").resolve(page.fileName))
            listOf(english, chinese).forEach { content ->
                assertTrue(content.contains("item_ids:"), page.fileName)
                page.itemIds.forEach { itemId ->
                    assertTrue(content.contains("- $itemId"), "$itemId in ${page.fileName}")
                }
                assertTrue(content.contains("<ItemImage id=\"${page.imageId}\""), page.fileName)
            }
            assertTrue(indexEnglish.contains("(${page.fileName})"), page.fileName)
            assertTrue(indexChinese.contains("(${page.fileName})"), page.fileName)
        }
    }

    @Test
    fun `every local item link has an indexed guide page`() {
        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))
        val guideRoot = projectRoot.resolve("mod/src/main/resources/assets/lazy/guides/lazy/guide")
        val englishPages =
            Files.list(guideRoot).use { paths ->
                paths.filter { it.fileName.toString().endsWith(".md") }.toList()
            }
        val indexedItems =
            englishPages
                .flatMap { path -> ITEM_ID_PATTERN.findAll(Files.readString(path)).map { it.groupValues[1] }.toList() }
                .toSet()
        val linkedItems =
            englishPages
                .flatMap { path -> ITEM_LINK_PATTERN.findAll(Files.readString(path)).map { it.groupValues[1] }.toList() }
                .toSet()

        assertTrue(indexedItems.containsAll(linkedItems), "Missing item guide pages: ${linkedItems - indexedItems}")
    }

    private data class MachinePage(
        val fileName: String,
        val itemId: String,
    )

    private companion object {
        val machinePages =
            listOf(
                MachinePage("machine_casing.md", "lazy:machine_casing"),
                MachinePage("buffer.md", "lazy:buffer"),
                MachinePage("energy_source.md", "lazy:energy_source"),
                MachinePage("item_copier.md", "lazy:item_copier"),
                MachinePage("repairer.md", "lazy:repairer"),
                MachinePage("shaper.md", "lazy:shaper"),
                MachinePage("simulation_chamber.md", "lazy:simulation_chamber"),
                MachinePage("essence_converter.md", "lazy:essence_converter"),
            )
        val toolPages =
            listOf(
                ToolPage("configuration_card.md", listOf("lazy:configuration_card"), "lazy:configuration_card"),
                ToolPage("data_model.md", listOf("lazy:data_model"), "lazy:data_model"),
                ToolPage("energy_battery.md", listOf("lazy:energy_battery"), "lazy:energy_battery"),
                ToolPage("modular_configurator.md", listOf("lazy:modular_configurator"), "lazy:modular_configurator"),
                ToolPage("teleporter.md", listOf("lazy:teleporter"), "lazy:teleporter"),
                ToolPage(
                    "processing_cores.md",
                    listOf(
                        "lazy:processing_core_t1",
                        "lazy:processing_core_t2",
                        "lazy:processing_core_t3",
                        "lazy:processing_core_t4",
                    ),
                    "lazy:processing_core_t1",
                ),
            )

        private data class ToolPage(
            val fileName: String,
            val itemIds: List<String>,
            val imageId: String,
        )

        private val ITEM_ID_PATTERN = Regex("^  - (lazy:[^\\s]+)$", RegexOption.MULTILINE)
        private val ITEM_LINK_PATTERN = Regex("<ItemLink id=\"(lazy:[^\"]+)\"")
    }
}
