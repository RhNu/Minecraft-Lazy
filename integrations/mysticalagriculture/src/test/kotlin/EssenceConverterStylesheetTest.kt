package rhx.lazy.integration.mysticalagriculture

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EssenceConverterStylesheetTest {
    @Test
    fun `essence converter stylesheet parses`() {
        val text =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/lss/essence_converter.lss"))
                .bufferedReader()
                .use { it.readText() }

        val stylesheet = Stylesheet.parse(text)
        val declarationCount = text.lineSequence().count { line -> line.trim().matches(DECLARATION) }

        assertTrue(stylesheet.rules.isNotEmpty())
        assertEquals(declarationCount, stylesheet.rules.sumOf { rule -> rule.properties.size })
    }

    private companion object {
        val DECLARATION = Regex("""[a-z-]+\s*:\s*.+;""")
    }
}
