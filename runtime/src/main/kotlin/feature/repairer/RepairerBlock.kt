package rhx.lazy.feature.repairer

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.MachineBlock
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class RepairerBlock :
    MachineBlock(
        Properties
            .of()
            .strength(0.5f)
            .sound(SoundType.METAL),
    ) {
    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity = RepairerBlockEntity(pos, state)

    override fun createUI(holder: BlockUIMenuType.BlockUIHolder): ModularUI = RepairerUI.create(holder)
}
