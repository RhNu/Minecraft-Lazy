package rhx.lazy.feature.energy

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.MachineBlock
import rhx.lazy.core.serverTicker

internal class EnergySourceBlock :
    MachineBlock(
        Properties
            .of()
            .strength(0.5f)
            .sound(SoundType.METAL),
    ) {
    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity = EnergySourceBlockEntity(pos, state)

    override fun createUI(holder: BlockUIMenuType.BlockUIHolder): ModularUI = EnergySourceUI.create(holder)

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = level.serverTicker(blockEntityType, EnergyRegistries.sourceBlockEntity.get()) { onServerTick() }
}
