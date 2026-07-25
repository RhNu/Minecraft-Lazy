package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import kotlin.test.Test
import kotlin.test.assertTrue

class EnergySourceBlockEntityPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `active push survives managed data round trip`() {
        val source =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
            )
        assertTrue(source.toggleActivePush())

        val restored =
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
            )
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        assertTrue(restored.isActivePushEnabled())
    }
}
