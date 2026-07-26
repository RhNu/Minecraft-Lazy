package rhx.lazy.feature.energy

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import rhx.lazy.core.ManagedBlockEntity
import rhx.lazy.core.storage.NetworkStorage
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult
import java.util.EnumMap

internal class EnergySourceBlockEntity(
    pos: BlockPos,
    state: BlockState,
    private val networkStorage: NetworkStoragePort = NetworkStorage,
) : ManagedBlockEntity(EnergyRegistries.sourceBlockEntity.get(), pos, state) {
    val energyStorage: IEnergyStorage = InfiniteEnergyStorage()

    @field:Persisted
    @field:LazyManaged
    private var activePushEnabled = false

    @field:Persisted
    @field:LazyManaged
    private var networkPushEnabled = false

    @field:Persisted
    @field:LazyManaged
    private var dimensionNetworkId = INVALID_NETWORK_ID

    private val neighborEnergyCaches =
        EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction?>>(Direction::class.java)

    fun isActivePushEnabled(): Boolean = activePushEnabled

    fun isNetworkPushEnabled(): Boolean = networkPushEnabled

    fun outputMode(): EnergyOutputMode =
        when {
            networkPushEnabled -> EnergyOutputMode.NETWORK
            activePushEnabled -> EnergyOutputMode.ACTIVE
            else -> EnergyOutputMode.PASSIVE
        }

    fun setOutputMode(
        mode: EnergyOutputMode,
        primaryNetworkId: NetworkStorageId? = null,
    ): Boolean {
        when (mode) {
            EnergyOutputMode.PASSIVE -> {
                activePushEnabled = false
                networkPushEnabled = false
                dimensionNetworkId = INVALID_NETWORK_ID
            }

            EnergyOutputMode.ACTIVE -> {
                activePushEnabled = true
                networkPushEnabled = false
                dimensionNetworkId = INVALID_NETWORK_ID
            }

            EnergyOutputMode.NETWORK -> {
                if (!networkStorage.isAvailable) return false
                val networkId = primaryNetworkId ?: return false
                activePushEnabled = false
                networkPushEnabled = true
                dimensionNetworkId = networkId.value
            }
        }
        outputStateChanged()
        return true
    }

    fun onServerTick() {
        if (networkPushEnabled) {
            pushToDimensionNetwork()
        }

        if (!activePushEnabled) return
        val serverLevel = level as? ServerLevel ?: return

        Direction.entries.forEach { direction ->
            val storage = cacheFor(serverLevel, direction).getCapability()
            if (storage?.canReceive() == true) {
                storage.receiveEnergy(ENERGY_TRANSFER_LIMIT, false)
            }
        }
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        neighborEnergyCaches.clear()
        if (networkPushEnabled && dimensionNetworkId < 0) {
            networkPushEnabled = false
            dimensionNetworkId = INVALID_NETWORK_ID
        }
        if (networkPushEnabled && activePushEnabled) {
            activePushEnabled = false
        }
    }

    override fun setRemoved() {
        neighborEnergyCaches.clear()
        super.setRemoved()
    }

    private fun cacheFor(
        level: ServerLevel,
        direction: Direction,
    ): BlockCapabilityCache<IEnergyStorage, Direction?> =
        neighborEnergyCaches.getOrPut(direction) {
            BlockCapabilityCache.create<IEnergyStorage, Direction?>(
                Capabilities.EnergyStorage.BLOCK,
                level,
                blockPos.relative(direction),
                direction.opposite,
                { !isRemoved },
                {},
            )
        }

    private fun pushToDimensionNetwork() {
        val networkId =
            if (dimensionNetworkId >= 0) {
                NetworkStorageId(dimensionNetworkId)
            } else {
                disableNetworkPush()
                return
            }

        when (
            networkStorage.insertEnergy(
                networkId,
                ENERGY_TRANSFER_LIMIT.toLong(),
                simulate = false,
            )
        ) {
            is NetworkStorageResult.Success -> Unit
            else -> disableNetworkPush()
        }
    }

    private fun disableNetworkPush() {
        if (!networkPushEnabled && dimensionNetworkId == INVALID_NETWORK_ID) return
        networkPushEnabled = false
        dimensionNetworkId = INVALID_NETWORK_ID
        outputStateChanged()
    }

    private fun outputStateChanged() {
        markDirty(ACTIVE_PUSH_FIELD)
        markDirty(NETWORK_PUSH_FIELD)
        markDirty(DIMENSION_NETWORK_ID_FIELD)
    }

    private companion object {
        const val ACTIVE_PUSH_FIELD = "activePushEnabled"
        const val NETWORK_PUSH_FIELD = "networkPushEnabled"
        const val DIMENSION_NETWORK_ID_FIELD = "dimensionNetworkId"
        const val INVALID_NETWORK_ID = -1
    }
}

internal enum class EnergyOutputMode {
    PASSIVE,
    ACTIVE,
    NETWORK,
}
