package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IoPanelStylesheetTest {
    @Test
    fun `io panel stylesheet selectors and declarations are valid`() {
        val declarationCount =
            STYLESHEET
                .lineSequence()
                .count { line -> line.trim().matches(DECLARATION) }
        val stylesheet = Stylesheet.parse(STYLESHEET)

        assertTrue(stylesheet.rules.isNotEmpty())
        // Every declaration must become a property: an unknown name is dropped without a word.
        assertEquals(declarationCount, stylesheet.rules.sumOf { rule -> rule.properties.size })
    }

    @Test
    fun `io panel stylesheet styles every widget the panel builds`() {
        SELECTORS.forEach { selector ->
            assertTrue(STYLESHEET.contains("$selector "), "missing rule for $selector")
        }
    }

    @Test
    fun `the tab body height belongs to the panel code`() {
        // IoPanelUI sizes the body to the tallest tab so switching tabs never resizes the window;
        // a height declared here would override that and bring the resizing back.
        val body = STYLESHEET.substringAfter(".lazy-io__body {").substringBefore('}')
        assertTrue(body.isNotBlank())
        assertTrue(!body.contains("height:"), "the body height belongs to IoPanelUI.bodyHeight")
    }

    private companion object {
        val STYLESHEET: String =
            requireNotNull(IoPanelStylesheetTest::class.java.getResourceAsStream("/assets/lazy/lss/io.lss"))
                .bufferedReader()
                .use { it.readText() }

        val DECLARATION = Regex("""[a-z-]+\s*:\s*.+;""")

        val SELECTORS =
            listOf(
                ".lazy-io__panel",
                ".lazy-io__title",
                ".lazy-io__tabs",
                ".lazy-io__tab",
                ".lazy-io__body",
                ".lazy-io__content",
                ".lazy-io__passive-icon",
                ".lazy-io__hint",
                ".lazy-io__face-grid",
                ".lazy-io__face-row",
                ".lazy-io__face-placeholder",
                ".lazy-io__face--none",
                ".lazy-io__face--input",
                ".lazy-io__face--output",
                ".lazy-io__face--both",
                ".lazy-io__eject",
                ".lazy-io__network-status",
                ".lazy-io__network-list",
                ".lazy-io__network-empty",
                ".lazy-io__provider",
                ".lazy-io__network-actions",
                ".lazy-io__network-action",
                ".lazy-io__button--selected",
            )
    }
}
