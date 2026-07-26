package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import rhx.lazy.integration.beyonddimensions.FakeDimensionNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnergySourceBlockEntityPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `active push survives managed data round trip`() {
        val storage = FakeDimensionNetworkStorage()
        val source =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
                storage,
            )
        assertEquals(EnergyOutputMode.ADJACENT, source.cycleOutputMode())
        assertEquals(
            EnergyOutputMode.NETWORK,
            source.cycleOutputMode(FakeDimensionNetworkStorage.TEST_NETWORK_ID),
        )
        assertEquals(EnergyOutputMode.BOTH, source.cycleOutputMode())

        val restored =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
                storage,
            )
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        assertTrue(restored.isActivePushEnabled())
        assertTrue(restored.isNetworkPushEnabled())
        assertEquals(EnergyOutputMode.BOTH, restored.outputMode())
    }
}
