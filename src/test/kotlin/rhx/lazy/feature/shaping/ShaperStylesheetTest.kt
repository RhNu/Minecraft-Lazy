package rhx.lazy.feature.shaping

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShaperStylesheetTest {
    @Test
    fun `shaper stylesheet selectors and declarations are valid`() {
        val text =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/lss/shaper.lss"))
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
