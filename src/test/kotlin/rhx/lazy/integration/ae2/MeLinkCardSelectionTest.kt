package rhx.lazy.integration.ae2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class MeLinkCardSelectionTest {
    @Test
    fun `main hand wins before offhand and inventory`() {
        val result = MeLinkCardSelection.select(listOf("main", "off"), listOf("inventory"))

        assertEquals("main", assertNotNull(result as? MeLinkCardSelection.Selected).target)
    }

    @Test
    fun `offhand wins when main hand is not linked`() {
        val result = MeLinkCardSelection.select(listOf(null, "off"), listOf("inventory"))

        assertEquals("off", assertNotNull(result as? MeLinkCardSelection.Selected).target)
    }

    @Test
    fun `duplicate inventory cards for one target are not ambiguous`() {
        val result = MeLinkCardSelection.select(emptyList(), listOf("same", "same"))

        assertEquals("same", assertNotNull(result as? MeLinkCardSelection.Selected).target)
    }

    @Test
    fun `different inventory targets require a held card`() {
        assertSame(
            MeLinkCardSelection.Ambiguous,
            MeLinkCardSelection.select(emptyList(), listOf("first", "second")),
        )
    }

    @Test
    fun `unlinked inventory has no target`() {
        assertSame(MeLinkCardSelection.Missing, MeLinkCardSelection.select<String>(listOf(null), emptyList()))
    }
}
