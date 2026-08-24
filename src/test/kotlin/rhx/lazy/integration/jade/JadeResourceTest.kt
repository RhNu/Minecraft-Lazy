package rhx.lazy.integration.jade

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JadeResourceTest {
    @Test
    fun `Jade provider names and tooltip text are localized`() {
        val english = readJson("/assets/lazy/lang/en_us.json")
        val chinese = readJson("/assets/lazy/lang/zh_cn.json")

        assertEquals("Buffer status", english["config.jade.plugin_lazy.buffer"].asString)
        assertEquals("缓冲器状态", chinese["config.jade.plugin_lazy.buffer"].asString)
        assertEquals("Shaper items", english["config.jade.plugin_lazy.shaper_item_storage"].asString)
        assertEquals("塑形机物品", chinese["config.jade.plugin_lazy.shaper_item_storage"].asString)
        assertEquals("Simulation Chamber items", english["config.jade.plugin_lazy.simulation_chamber_item_storage"].asString)
        assertEquals("模拟室物品", chinese["config.jade.plugin_lazy.simulation_chamber_item_storage"].asString)
        assertEquals("Output: %s", english["jade.lazy.output"].asString)
        assertEquals("输出：%s", chinese["jade.lazy.output"].asString)
        assertEquals("Face output • eject %s", english["jade.lazy.output.face"].asString)
        assertEquals("面输出 • 弹出 %s", chinese["jade.lazy.output.face"].asString)
        assertEquals("Items: %s/%s • Fluid: %s/%s mB", english["jade.lazy.buffer.contents"].asString)
        assertEquals("物品：%s/%s • 流体：%s/%s mB", chinese["jade.lazy.buffer.contents"].asString)
        assertEquals("Generation interval: %s ticks", english["jade.lazy.item_copier.generation_interval"].asString)
        assertEquals("生成间隔：%s 刻", chinese["jade.lazy.item_copier.generation_interval"].asString)
        assertEquals("Essence: %s • remainder: %s", english["jade.lazy.essence_converter.contents"].asString)
        assertEquals("精华：%s • 余量：%s", chinese["jade.lazy.essence_converter.contents"].asString)
    }

    @Test
    fun `Jade is declared as an optional two-sided dependency`() {
        val metadata =
            requireNotNull(javaClass.getResourceAsStream("/META-INF/neoforge.mods.toml"))
                .bufferedReader()
                .use { it.readText() }
        val jadeDependency = metadata.substringAfter("modId=\"jade\"").substringBefore("[[dependencies.lazy]]")

        assertTrue("type=\"optional\"" in jadeDependency)
        assertTrue("versionRange=\"[15.10.5,16.0.0)\"" in jadeDependency)
        assertTrue("side=\"BOTH\"" in jadeDependency)
    }

    private fun readJson(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { reader -> JsonParser.parseReader(reader).asJsonObject }
}
