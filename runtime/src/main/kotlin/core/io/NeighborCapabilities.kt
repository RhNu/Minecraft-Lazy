package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.capabilities.BlockCapability
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler
import java.util.EnumMap
import java.util.function.BooleanSupplier

/**
 * Per-direction [BlockCapabilityCache] pool for a machine that ejects into its neighbours.
 *
 * Machines share this instead of hand-rolling the same EnumMap plumbing; caches are created on first
 * use and dropped by [invalidate] when the owner is removed or reloaded.
 */
internal class NeighborCapabilities<T>(
    private val capability: BlockCapability<T, Direction?>,
    private val origin: BlockPos,
    stillValid: () -> Boolean,
) {
    private val caches = EnumMap<Direction, BlockCapabilityCache<T, Direction?>>(Direction::class.java)
    private val validity = BooleanSupplier(stillValid)

    operator fun get(
        level: ServerLevel,
        direction: Direction,
    ): T? =
        caches
            .getOrPut(direction) {
                BlockCapabilityCache.create(capability, level, origin.relative(direction), direction.opposite, validity) {}
            }.capability

    fun invalidate() {
        caches.clear()
    }

    companion object {
        fun items(
            origin: BlockPos,
            stillValid: () -> Boolean,
        ): NeighborCapabilities<IItemHandler> = NeighborCapabilities(Capabilities.ItemHandler.BLOCK, origin, stillValid)

        fun fluids(
            origin: BlockPos,
            stillValid: () -> Boolean,
        ): NeighborCapabilities<IFluidHandler> = NeighborCapabilities(Capabilities.FluidHandler.BLOCK, origin, stillValid)

        fun energy(
            origin: BlockPos,
            stillValid: () -> Boolean,
        ): NeighborCapabilities<IEnergyStorage> = NeighborCapabilities(Capabilities.EnergyStorage.BLOCK, origin, stillValid)
    }
}
