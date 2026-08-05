package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import rhx.lazy.core.io.IoRoute
import rhx.lazy.core.testing.FakeNetworkOutputProvider
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnergySourceBlockEntityPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `network push survives managed data round trip`() {
        val storage = FakeNetworkStorage()
        val provider = FakeNetworkOutputProvider(storage)
        val source =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
            )
        assertTrue(source.ioController.setNetworkTarget(provider.target))

        val restored =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
            )
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        assertEquals(IoRoute.NETWORK, restored.ioController.route)
    }
}
