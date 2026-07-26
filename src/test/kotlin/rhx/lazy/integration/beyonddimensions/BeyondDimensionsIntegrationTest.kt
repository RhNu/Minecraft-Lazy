package rhx.lazy.integration.beyonddimensions

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BeyondDimensionsIntegrationTest {
    @Test
    fun `facade remains callable without Beyond Dimensions on the runtime classpath`() {
        assertFalse(BeyondDimensionsIntegration.isAvailable)
        assertTrue(
            BeyondDimensionsIntegration.itemAmount(
                DimensionNetworkId(0),
                ItemStack(Items.STONE),
            ) === DimensionNetworkResult.IntegrationUnavailable,
        )
    }
}
