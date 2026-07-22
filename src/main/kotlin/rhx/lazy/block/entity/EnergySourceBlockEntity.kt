package rhx.lazy.block.entity

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import rhx.lazy.registry.ModBlockEntities
import rhx.lazy.util.ENERGY_TRANSFER_LIMIT
import rhx.lazy.util.InfiniteEnergyStorage
import java.util.EnumMap

internal class EnergySourceBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(ModBlockEntities.energySource.get(), pos, state) {
    val energyStorage: IEnergyStorage = InfiniteEnergyStorage()

    private var activePushEnabled = false
    private val neighborEnergyCaches =
        EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction?>>(Direction::class.java)

    fun toggleActivePush(): Boolean {
        activePushEnabled = !activePushEnabled
        setChanged()
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

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        tag.putBoolean(ACTIVE_PUSH_KEY, activePushEnabled)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        activePushEnabled = tag.getBoolean(ACTIVE_PUSH_KEY)
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
        const val ACTIVE_PUSH_KEY = "ActivePush"
    }
}
