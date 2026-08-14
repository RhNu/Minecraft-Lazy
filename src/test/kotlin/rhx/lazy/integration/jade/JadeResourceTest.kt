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
