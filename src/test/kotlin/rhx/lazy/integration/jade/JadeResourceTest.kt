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
        assertEquals("Output mode: %s", english["jade.lazy.energy_source.output_mode"].asString)
        assertEquals("输出模式：%s", chinese["jade.lazy.energy_source.output_mode"].asString)
        assertEquals("Growth: %s%%", english["jade.lazy.planter.growth"].asString)
        assertEquals("生长进度：%s%%", chinese["jade.lazy.planter.growth"].asString)
        assertEquals("Total pot bonus: ×%s", english["jade.lazy.planter.pot_bonus"].asString)
        assertEquals("盆栽总加成：×%s", chinese["jade.lazy.planter.pot_bonus"].asString)
        assertEquals("Output mode: %s", english["jade.lazy.planter.output_mode"].asString)
        assertEquals("输出模式：%s", chinese["jade.lazy.planter.output_mode"].asString)
        assertEquals("Passive", english["jade.lazy.planter.mode.passive"].asString)
        assertEquals("被动", chinese["jade.lazy.planter.mode.passive"].asString)
        assertEquals("Downward output", english["jade.lazy.planter.mode.downward"].asString)
        assertEquals("向下输出", chinese["jade.lazy.planter.mode.downward"].asString)
        assertEquals("Output to %s", english["jade.lazy.planter.mode.network"].asString)
        assertEquals("输出到%s", chinese["jade.lazy.planter.mode.network"].asString)
        assertTrue(!english.has("jade.lazy.planter.pending"))
        assertTrue(!english.has("jade.lazy.planter.outputs"))
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
