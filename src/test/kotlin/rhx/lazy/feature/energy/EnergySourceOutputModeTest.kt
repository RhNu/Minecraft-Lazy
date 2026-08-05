package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import rhx.lazy.core.io.IoRoute
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.testing.FakeNetworkOutputProvider
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnergySourceOutputModeTest {
    @Test
    fun `output modes are mutually exclusive`() {
        val source = newSource()

        assertEquals(IoRoute.PASSIVE, source.ioController.route)

        assertTrue(source.ioController.setRoute(IoRoute.ADJACENT))
        assertEquals(IoRoute.ADJACENT, source.ioController.route)

        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())
        assertTrue(source.ioController.setNetworkTarget(provider.target))
        assertEquals(IoRoute.NETWORK, source.ioController.route)

        source.ioController.setPassive()
        assertEquals(IoRoute.PASSIVE, source.ioController.route)
    }

    @Test
    fun `network mode rejects malformed provider targets`() {
        val source = newSource()
        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())

        assertFalse(source.ioController.setNetworkTarget(NetworkTargetRef(provider.id, CompoundTag())))
        assertEquals(IoRoute.PASSIVE, source.ioController.route)
    }

    @Test
    fun `network output sends configured energy and stale binding degrades to passive`() {
        val storage = FakeNetworkStorage()
        val source = newSource()
        val provider = FakeNetworkOutputProvider(storage)
        source.ioController.setNetworkTarget(provider.target)

        source.onServerTick()
        assertEquals(ENERGY_TRANSFER_LIMIT.toLong(), storage.storedEnergy)
        assertEquals(IoRoute.NETWORK, source.ioController.route)

        storage.networkExists = false
        source.onServerTick()

        assertEquals(IoRoute.PASSIVE, source.ioController.route)
    }

    private fun newSource(): EnergySourceBlockEntity =
        EnergySourceBlockEntity(
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
        )
}
