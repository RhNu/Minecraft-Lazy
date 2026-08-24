package rhx.lazy.core.config

import com.electronwill.nightconfig.core.CommentedConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LazyConfigTest {
    @Test
    fun `values expose defaults before NeoForge loads the config`() {
        val definition =
            LazyConfigDefinition.create { builder ->
                builder.int("integer", 4, 1..10, "Integer comment")
            }

        assertEquals(4, definition.settings.get())
    }

    @Test
    fun `spec fills defaults for every supported value type`() {
        val definition = sampleDefinition()
        val config = CommentedConfig.inMemory()

        definition.spec.correct(config)

        assertEquals(4, config.get<Int>("integer"))
        assertEquals(8L, config.get<Long>("long"))
        assertEquals(true, config.get<Boolean>("boolean"))
        assertEquals(listOf("lazy", "minecraft"), config.get<List<String>>("strings"))
    }

    @Test
    fun `spec replaces invalid values and preserves valid lists`() {
        val definition = sampleDefinition()
        val config =
            CommentedConfig.inMemory().apply {
                set<Int>("integer", 100)
                set<Long>("long", 0L)
                set<Boolean>("boolean", "not a boolean")
                set<List<String>>("strings", listOf("kubejs"))
            }

        definition.spec.correct(config)

        assertEquals(10, config.get<Int>("integer"))
        assertEquals(1L, config.get<Long>("long"))
        assertEquals(true, config.get<Boolean>("boolean"))
        assertEquals(listOf("kubejs"), config.get<List<String>>("strings"))
    }

    @Test
    fun `spec records comments and rejects non-string list entries`() {
        val definition = sampleDefinition()
        val config =
            CommentedConfig.inMemory().apply {
                set<List<Any>>("strings", listOf("lazy", 1))
            }

        definition.spec.correct(config)

        assertTrue(config.getComment("integer").orEmpty().contains("Integer comment"))
        assertEquals(listOf("lazy"), config.get<List<String>>("strings"))
    }

    private fun sampleDefinition(): LazyConfigDefinition<Unit> =
        LazyConfigDefinition.create<Unit> { builder ->
            builder.int("integer", 4, 1..10, "Integer comment")
            builder.long("long", 8L, 1L..16L, "Long comment")
            builder.boolean("boolean", true, "Boolean comment")
            builder.stringList("strings", listOf("lazy", "minecraft"), "String list comment")
        }
}
