package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import rhx.lazy.feature.itemcopier.ItemCopierBlockEntity
import rhx.lazy.feature.itemcopier.ItemCopierRegistries
import kotlin.test.Test
import kotlin.test.assertEquals

class IoAdapterSupportTest {
    @Test
    fun `machine adapters expose the planned routes and default to passive`() {
        assertRoutes(
            EnergySourceBlockEntity(
                BlockPos.ZERO,
                EnergyRegistries.sourceBlock.get().defaultBlockState(),
            ),
            IoRoute.PASSIVE,
            IoRoute.ADJACENT,
            IoRoute.NETWORK,
        )
        assertRoutes(
            BufferBlockEntity(
                BlockPos.ZERO,
                BufferRegistries.block.get().defaultBlockState(),
            ),
            IoRoute.PASSIVE,
            IoRoute.DOWNWARD,
            IoRoute.NETWORK,
        )
        assertRoutes(
            ItemCopierBlockEntity(
                BlockPos.ZERO,
                ItemCopierRegistries.block.get().defaultBlockState(),
            ),
            IoRoute.PASSIVE,
            IoRoute.ADJACENT,
            IoRoute.NETWORK,
        )
    }

    private fun assertRoutes(
        blockEntity: IoManagedBlockEntity,
        vararg routes: IoRoute,
    ) {
        assertEquals(IoRoute.PASSIVE, blockEntity.ioController.route)
        assertEquals(routes.toSet(), blockEntity.ioController.supportedRoutes)
    }
}
