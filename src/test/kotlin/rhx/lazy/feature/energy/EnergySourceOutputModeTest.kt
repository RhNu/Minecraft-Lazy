package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnergySourceOutputModeTest {
    @Test
    fun `available integration cycles through all four output modes`() {
        val source = newSource(FakeNetworkStorage())

        assertEquals(EnergyOutputMode.ADJACENT, source.cycleOutputMode())
        assertTrue(source.nextModeNeedsNetwork())
        assertNull(source.cycleOutputMode())
        assertEquals(EnergyOutputMode.ADJACENT, source.outputMode())
        assertEquals(
            EnergyOutputMode.NETWORK,
            source.cycleOutputMode(FakeNetworkStorage.TEST_NETWORK_ID),
        )
        assertEquals(EnergyOutputMode.BOTH, source.cycleOutputMode())
        assertEquals(EnergyOutputMode.OFF, source.cycleOutputMode())
    }

    @Test
    fun `unavailable integration preserves the original two mode cycle`() {
        val source = newSource(FakeNetworkStorage(isAvailable = false))

        assertEquals(EnergyOutputMode.ADJACENT, source.cycleOutputMode())
        assertEquals(EnergyOutputMode.OFF, source.cycleOutputMode())
        assertFalse(source.isNetworkPushEnabled())
    }

    @Test
    fun `network output sends configured energy and stale binding degrades to adjacent`() {
        val storage = FakeNetworkStorage()
        val source = newSource(storage)
        source.cycleOutputMode()
        source.cycleOutputMode(FakeNetworkStorage.TEST_NETWORK_ID)
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

    private fun newSource(storage: FakeNetworkStorage): EnergySourceBlockEntity =
        EnergySourceBlockEntity(
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
            storage,
        )
}
