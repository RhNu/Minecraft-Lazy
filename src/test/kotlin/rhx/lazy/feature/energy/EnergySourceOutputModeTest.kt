package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import rhx.lazy.integration.beyonddimensions.FakeDimensionNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnergySourceOutputModeTest {
    @Test
    fun `available integration cycles through all four output modes`() {
        val source = newSource(FakeDimensionNetworkStorage())

        assertEquals(EnergyOutputMode.ADJACENT, source.cycleOutputMode())
        assertTrue(source.nextModeNeedsNetwork())
        assertNull(source.cycleOutputMode())
        assertEquals(EnergyOutputMode.ADJACENT, source.outputMode())
        assertEquals(
            EnergyOutputMode.NETWORK,
            source.cycleOutputMode(FakeDimensionNetworkStorage.TEST_NETWORK_ID),
        )
        assertEquals(EnergyOutputMode.BOTH, source.cycleOutputMode())
        assertEquals(EnergyOutputMode.OFF, source.cycleOutputMode())
    }

    @Test
    fun `unavailable integration preserves the original two mode cycle`() {
        val source = newSource(FakeDimensionNetworkStorage(isAvailable = false))

        assertEquals(EnergyOutputMode.ADJACENT, source.cycleOutputMode())
        assertEquals(EnergyOutputMode.OFF, source.cycleOutputMode())
        assertFalse(source.isNetworkPushEnabled())
    }

    @Test
    fun `network output sends configured energy and stale binding degrades to adjacent`() {
        val storage = FakeDimensionNetworkStorage()
        val source = newSource(storage)
        source.cycleOutputMode()
        source.cycleOutputMode(FakeDimensionNetworkStorage.TEST_NETWORK_ID)
        source.cycleOutputMode()

        source.onServerTick()
        assertEquals(ENERGY_TRANSFER_LIMIT.toLong(), storage.storedEnergy)
        assertEquals(EnergyOutputMode.BOTH, source.outputMode())

        storage.networkExists = false
        source.onServerTick()

        assertEquals(EnergyOutputMode.ADJACENT, source.outputMode())
        assertTrue(source.isActivePushEnabled())
        assertFalse(source.isNetworkPushEnabled())
    }

    private fun newSource(storage: FakeDimensionNetworkStorage): EnergySourceBlockEntity =
        EnergySourceBlockEntity(
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
            storage,
        )
}
