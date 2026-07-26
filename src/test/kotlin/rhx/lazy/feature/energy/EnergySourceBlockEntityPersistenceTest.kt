package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnergySourceBlockEntityPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `network push survives managed data round trip`() {
        val storage = FakeNetworkStorage()
        val source =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
                storage,
            )
        assertTrue(
            source.setOutputMode(
                EnergyOutputMode.NETWORK,
                FakeNetworkStorage.TEST_NETWORK_ID,
            ),
        )

        val restored =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
                storage,
            )
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        assertFalse(restored.isActivePushEnabled())
        assertTrue(restored.isNetworkPushEnabled())
        assertEquals(EnergyOutputMode.NETWORK, restored.outputMode())
    }
}
