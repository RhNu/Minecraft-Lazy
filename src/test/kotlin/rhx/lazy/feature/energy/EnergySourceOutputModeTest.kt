package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import rhx.lazy.core.io.IoMode
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

        assertEquals(IoMode.PASSIVE, source.ioController.mode)

        source.ioController.setMode(IoMode.FACE)
        assertEquals(IoMode.FACE, source.ioController.mode)

        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())
        assertTrue(source.ioController.setNetworkTarget(provider.target))
        assertEquals(IoMode.NETWORK, source.ioController.mode)

        source.ioController.setMode(IoMode.PASSIVE)
        assertEquals(IoMode.PASSIVE, source.ioController.mode)
    }

    @Test
    fun `network mode rejects malformed provider targets`() {
        val source = newSource()
        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())

        assertFalse(source.ioController.setNetworkTarget(NetworkTargetRef(provider.id, CompoundTag())))
        assertEquals(IoMode.PASSIVE, source.ioController.mode)
    }

    @Test
    fun `network output sends configured energy and stale binding degrades to passive`() {
        val storage = FakeNetworkStorage()
        val source = newSource()
        val provider = FakeNetworkOutputProvider(storage)
        source.ioController.setNetworkTarget(provider.target)

        source.onServerTick()
        assertEquals(ENERGY_TRANSFER_LIMIT.toLong(), storage.storedEnergy)
        assertEquals(IoMode.NETWORK, source.ioController.mode)

        storage.networkExists = false
        source.onServerTick()

        assertEquals(IoMode.PASSIVE, source.ioController.mode)
    }

    private fun newSource(): EnergySourceBlockEntity =
        EnergySourceBlockEntity(
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
        )
}
