package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoConfiguration
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoMode
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputRouter
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTransferResult
import java.util.EnumMap

internal class EnergySourceBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(EnergyRegistries.sourceBlockEntity.get(), pos, state) {
    val energyStorage: IEnergyStorage = InfiniteEnergyStorage()

    private val neighborEnergyCaches =
        EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction?>>(Direction::class.java)

    private val ioAdapter = EnergyIoAdapter()

    init {
        installIoAdapter(ioAdapter)
    }

    fun onServerTick() {
        ioController.tick()
    }

    override fun setRemoved() {
        neighborEnergyCaches.clear()
        super.setRemoved()
    }

    private inner class EnergyIoAdapter : IoAdapter {
        override val capabilities = setOf(NetworkInsertCapabilities.ENERGY)

        override fun push(configuration: IoConfiguration): IoPushResult =
            when (configuration.mode) {
                IoMode.FACE -> pushToFaces(ioController.outputDirections())
                IoMode.NETWORK -> pushToNetwork(configuration.networkTarget)
                else -> IoPushResult.Success
            }

        private fun pushToFaces(directions: Set<Direction>): IoPushResult {
            if (directions.isEmpty()) return IoPushResult.Success
            val serverLevel = level as? ServerLevel ?: return IoPushResult.Retry
            directions.forEach { direction ->
                val storage = cacheFor(serverLevel, direction).getCapability()
                if (storage?.canReceive() == true) {
                    storage.receiveEnergy(ENERGY_TRANSFER_LIMIT, false)
                }
            }
            return IoPushResult.Success
        }

        private fun pushToNetwork(target: NetworkTargetRef?): IoPushResult {
            val networkTarget = target ?: return IoPushResult.TargetMissing
            return when (
                val result =
                    NetworkOutputRouter.insert(
                        networkTarget,
                        NetworkPayload.Energy(ENERGY_TRANSFER_LIMIT.toLong()),
                        simulate = false,
                    )
            ) {
                is NetworkTransferResult.Success -> IoPushResult.Success
                NetworkTransferResult.TargetMissing -> IoPushResult.TargetMissing
                NetworkTransferResult.InvalidTarget -> IoPushResult.TargetMissing
                NetworkTransferResult.OutcomeUnknown -> IoPushResult.OutcomeUnknown
                NetworkTransferResult.TemporarilyUnavailable -> IoPushResult.Retry
            }
        }

        private fun cacheFor(
            serverLevel: ServerLevel,
            direction: Direction,
        ): BlockCapabilityCache<IEnergyStorage, Direction?> =
            neighborEnergyCaches.getOrPut(direction) {
                BlockCapabilityCache.create<IEnergyStorage, Direction?>(
                    Capabilities.EnergyStorage.BLOCK,
                    serverLevel,
                    blockPos.relative(direction),
                    direction.opposite,
                    { !isRemoved },
                    {},
                )
            }
    }
}
