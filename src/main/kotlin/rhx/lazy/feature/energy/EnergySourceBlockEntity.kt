package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.energy.IEnergyStorage
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NeighborCapabilities
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputRouter
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.toPushResult

internal class EnergySourceBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(EnergyRegistries.sourceBlockEntity.get(), pos, state) {
    val energyStorage: IEnergyStorage = InfiniteEnergyStorage()

    private val neighborEnergy = NeighborCapabilities.energy(blockPos) { !isRemoved }

    init {
        installIoAdapter(EnergyIoAdapter())
    }

    fun onServerTick() {
        ioController.tick()
    }

    override fun setRemoved() {
        neighborEnergy.invalidate()
        super.setRemoved()
    }

    private inner class EnergyIoAdapter : IoAdapter {
        override val capabilities = setOf(NetworkInsertCapabilities.ENERGY)
        override val acceptsInput = false

        override fun pushToFaces(directions: Set<Direction>): IoPushResult {
            val serverLevel = level as? ServerLevel ?: return IoPushResult.Retry
            directions.forEach { direction ->
                val storage = neighborEnergy[serverLevel, direction]
                if (storage?.canReceive() == true) storage.receiveEnergy(ENERGY_TRANSFER_LIMIT, false)
            }
            return IoPushResult.Success
        }

        override fun pushToNetwork(target: NetworkTargetRef): IoPushResult =
            NetworkOutputRouter
                .insert(target, NetworkPayload.Energy(ENERGY_TRANSFER_LIMIT.toLong()), false)
                .toPushResult()
    }
}
