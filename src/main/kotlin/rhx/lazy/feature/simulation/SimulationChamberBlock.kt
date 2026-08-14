package rhx.lazy.feature.simulation

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.serverTicker
import rhx.lazy.core.sidedUseResult

internal class SimulationChamberBlock :
    Block(Properties.of().strength(1.5f).sound(SoundType.METAL)),
    EntityBlock,
    BlockUIMenuType.BlockUI {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity = SimulationChamberBlockEntity(pos, state)

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun rotate(
        state: BlockState,
        rotation: Rotation,
    ): BlockState = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(
        state: BlockState,
        mirror: Mirror,
    ): BlockState = rotate(state, mirror.getRotation(state.getValue(FACING)))

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean,
    ) {
        if (!level.isClientSide && state.block !== newState.block) {
            level.blockEntityOrNull(pos, SimulationRegistries.blockEntity.get())?.let { blockEntity ->
                val drop = ItemStack(SimulationRegistries.item.get())
                if (blockEntity.hasContents()) blockEntity.saveToItemWithoutNetwork(drop, level.registryAccess())
                popResource(level, pos, drop)
            }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult,
    ): InteractionResult =
        level.sidedUseResult(
            if (level.isClientSide) {
                true
            } else {
                BlockUIMenuType.openUI(player as ServerPlayer, pos)
            },
        )

    override fun createUI(holder: BlockUIMenuType.BlockUIHolder): ModularUI = SimulationChamberUI.create(holder)

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = level.serverTicker(blockEntityType, SimulationRegistries.blockEntity.get()) { serverTick() }

    override fun stillValid(holder: BlockUIMenuType.BlockUIHolder): Boolean =
        holder.player
            .level()
            .getBlockState(holder.pos)
            .`is`(this) &&
            holder.player.distanceToSqr(holder.pos.center) <= 64.0

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }
}
