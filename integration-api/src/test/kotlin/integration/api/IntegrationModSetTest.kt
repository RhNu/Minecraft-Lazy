package rhx.lazy.integration.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationModSetTest {
    @Test
    fun `the same mod snapshot can be installed by common and client entrypoints`() {
        val loadedMods = setOf("lazy", "curios")

        IntegrationModSet.install(loadedMods)
        IntegrationModSet.install(loadedMods.toMutableSet())

        assertEquals(loadedMods, IntegrationModSet.loadedMods)
        val rejectedDifferentSnapshot =
            try {
                IntegrationModSet.install(loadedMods + "mekanism")
                false
            } catch (_: IllegalStateException) {
                true
            }
        assertTrue(rejectedDifferentSnapshot)
    }
}
