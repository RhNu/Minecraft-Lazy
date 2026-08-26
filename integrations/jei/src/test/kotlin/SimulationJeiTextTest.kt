package rhx.lazy.integration.jei

import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationJeiTextTest {
    @Test
    fun `group resource location is converted to a supported translation argument`() {
        val tooltip = groupTooltip(ResourceLocation.fromNamespaceAndPath("lazy", "entity"))
        assertTrue(tooltip.contents is TranslatableContents)
        val contents = tooltip.contents as TranslatableContents

        assertEquals("lazy:entity", contents.args.single())
    }
}
