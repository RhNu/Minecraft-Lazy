package rhx.lazy.feature.repairer

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepairerStylesheetTest {
    @Test
    fun `repairer stylesheet selectors and declarations are valid`() {
        val text =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/lss/repairer.lss"))
                .bufferedReader()
                .use { it.readText() }
        val stylesheet = Stylesheet.parse(text)
        val declarationCount =
            text
                .lineSequence()
                .count { line -> line.trim().matches(DECLARATION) }

        assertTrue(stylesheet.rules.isNotEmpty())
        assertEquals(declarationCount, stylesheet.rules.sumOf { rule -> rule.properties.size })
    }

    private companion object {
        val DECLARATION = Regex("""[a-z-]+\s*:\s*.+;""")
    }
}
