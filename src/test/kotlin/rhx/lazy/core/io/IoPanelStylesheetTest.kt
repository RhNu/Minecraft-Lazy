package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IoPanelStylesheetTest {
    @Test
    fun `io panel stylesheet parses`() {
        val text =
            requireNotNull(javaClass.getResourceAsStream("/assets/lazy/lss/io.lss"))
                .bufferedReader()
                .use { it.readText() }
        val stylesheet = Stylesheet.parse(text)
        assertTrue(stylesheet.rules.isNotEmpty())
        assertTrue(text.contains(".lazy-io__panel"))
        assertTrue(text.contains(".lazy-io__network-popup"))
        assertTrue(text.contains(".lazy-io__button--selected"))
        assertTrue(text.contains("base-background: built-in(ui-mc:RECT_BORDER)"))
        assertFalse(text.contains("text-wrap: wrap"))
        assertFalse(text.contains("max-width: 120"))
        assertFalse(text.contains("width: 190"))
        assertFalse(text.contains("width: 180"))
    }
}
