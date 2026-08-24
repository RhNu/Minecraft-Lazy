package rhx.lazy.integration.ae2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ConfigurationCardTargetSelectionTest {
    @Test
    fun `main hand wins before offhand and inventory`() {
        val result = ConfigurationCardTargetSelection.select(listOf("main", "off"), listOf("inventory"))

        assertEquals("main", assertNotNull(result as? ConfigurationCardTargetSelection.Selected).target)
    }

    @Test
    fun `duplicate inventory cards for one target are not ambiguous`() {
        val result = ConfigurationCardTargetSelection.select(emptyList(), listOf("same", "same"))

        assertEquals("same", assertNotNull(result as? ConfigurationCardTargetSelection.Selected).target)
    }

    @Test
    fun `different inventory targets require a held card`() {
        assertSame(
            ConfigurationCardTargetSelection.Ambiguous,
            ConfigurationCardTargetSelection.select(emptyList(), listOf("first", "second")),
        )
    }

    @Test
    fun `unlinked inventory has no target`() {
        assertSame(
            ConfigurationCardTargetSelection.Missing,
            ConfigurationCardTargetSelection.select<String>(listOf(null), emptyList()),
        )
    }
}
