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
import java.util.EnumMap

internal class EnergySourceBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : ManagedBlockEntity(EnergyRegistries.sourceBlockEntity.get(), pos, state) {
    val energyStorage: IEnergyStorage = InfiniteEnergyStorage()

    @field:Persisted
    @field:LazyManaged
    private var activePushEnabled = false
    private val neighborEnergyCaches =
        EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction?>>(Direction::class.java)

    fun isActivePushEnabled(): Boolean = activePushEnabled

    fun toggleActivePush(): Boolean {
        activePushEnabled = !activePushEnabled
        markDirty(ACTIVE_PUSH_FIELD)
        return activePushEnabled
    }

    fun onServerTick() {
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

    private companion object {
        const val ACTIVE_PUSH_FIELD = "activePushEnabled"
    }
}
