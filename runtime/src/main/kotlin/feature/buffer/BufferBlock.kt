package rhx.lazy.feature.buffer

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.MachineBlock
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.serverTicker
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class BufferBlock :
    MachineBlock(
        Properties
            .of()
            .strength(0.5f)
            .sound(SoundType.METAL),
    ) {
    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity = BufferBlockEntity(pos, state)

    override fun createUI(holder: BlockUIMenuType.BlockUIHolder): ModularUI = BufferUI.create(holder)

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? =
        level.serverTicker(blockEntityType, BufferRegistries.blockEntity.get()) {
            onServerTick()
        }

    /** Sneaking reports the totals in the action bar instead of opening the screen. */
    override fun handleUse(
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        if (!player.isShiftKeyDown) return super.handleUse(level, pos, player)
        val blockEntity = level.blockEntityOrNull(pos, BufferRegistries.blockEntity.get()) ?: return false
        if (level.isClientSide) return true
        player.displayActionBar(
            "message.lazy.buffer.status",
            blockEntity.totalItemCount,
            BufferBlockEntity.TOTAL_ITEM_CAPACITY,
            blockEntity.totalFluidAmount,
            BufferBlockEntity.TOTAL_FLUID_CAPACITY,
        )
        return true
    }
}
