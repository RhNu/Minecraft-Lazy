package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnergySourceOutputModeTest {
    @Test
    fun `output modes are mutually exclusive`() {
        val source = newSource(FakeNetworkStorage())

        assertEquals(EnergyOutputMode.PASSIVE, source.outputMode())

        assertTrue(source.setOutputMode(EnergyOutputMode.ACTIVE))
        assertEquals(EnergyOutputMode.ACTIVE, source.outputMode())
        assertTrue(source.isActivePushEnabled())
        assertFalse(source.isNetworkPushEnabled())

        assertTrue(
            source.setOutputMode(
                EnergyOutputMode.NETWORK,
                FakeNetworkStorage.TEST_NETWORK_ID,
            ),
        )
        assertEquals(EnergyOutputMode.NETWORK, source.outputMode())
        assertFalse(source.isActivePushEnabled())
        assertTrue(source.isNetworkPushEnabled())

        assertTrue(source.setOutputMode(EnergyOutputMode.PASSIVE))
        assertEquals(EnergyOutputMode.PASSIVE, source.outputMode())
        assertFalse(source.isActivePushEnabled())
        assertFalse(source.isNetworkPushEnabled())
    }

    @Test
    fun `network mode requires an available integration and selected network`() {
        val availableSource = newSource(FakeNetworkStorage())
        val source = newSource(FakeNetworkStorage(isAvailable = false))

        assertFalse(availableSource.setOutputMode(EnergyOutputMode.NETWORK))
        assertEquals(EnergyOutputMode.PASSIVE, availableSource.outputMode())
        assertFalse(
            source.setOutputMode(
                EnergyOutputMode.NETWORK,
                FakeNetworkStorage.TEST_NETWORK_ID,
            ),
        )
        assertEquals(EnergyOutputMode.PASSIVE, source.outputMode())
        assertFalse(source.isNetworkPushEnabled())
    }

    @Test
    fun `network output sends configured energy and stale binding degrades to passive`() {
        val storage = FakeNetworkStorage()
        val source = newSource(storage)
        source.setOutputMode(
            EnergyOutputMode.NETWORK,
            FakeNetworkStorage.TEST_NETWORK_ID,
        )

        source.onServerTick()
        assertEquals(ENERGY_TRANSFER_LIMIT.toLong(), storage.storedEnergy)
        assertEquals(EnergyOutputMode.NETWORK, source.outputMode())

        storage.networkExists = false
        source.onServerTick()

        assertEquals(EnergyOutputMode.PASSIVE, source.outputMode())
        assertFalse(source.isActivePushEnabled())
        assertFalse(source.isNetworkPushEnabled())
    }

    private fun newSource(storage: FakeNetworkStorage): EnergySourceBlockEntity =
        EnergySourceBlockEntity(
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
            storage,
        )
}
