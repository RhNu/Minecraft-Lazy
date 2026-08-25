package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.energy.IEnergyStorage
import rhx.lazy.core.io.InfiniteOutputSource
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.resource.ResourceKinds
import rhx.lazy.core.resource.energyAmount
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class EnergySourceBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(EnergyRegistries.sourceBlockEntity.get(), pos, state) {
    val energyStorage: IEnergyStorage = InfiniteEnergyStorage()
    private val outputSource = InfiniteOutputSource { listOf(requireNotNull(energyAmount(ENERGY_TRANSFER_LIMIT.toLong()))) }

    init {
        installIoAdapter(EnergyIoAdapter())
    }

    fun onServerTick() {
        ioController.tick()
    }

    private inner class EnergyIoAdapter : IoAdapter {
        override val capabilities = setOf(ResourceKinds.ENERGY)
        override val acceptsInput = false
        override val outputSource = this@EnergySourceBlockEntity.outputSource
    }
}
