package rhx.lazy.integration.mysticalagriculture

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.LivingEntity
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
import rhx.lazy.core.io.applyConfigurationCardOnPlacement
import rhx.lazy.core.io.ioCardInteraction
import rhx.lazy.core.sidedItemUseResult
import rhx.lazy.core.sidedUseResult

internal class EssenceConverterBlock :
    Block(
        Properties
            .of()
            .strength(0.5f)
            .sound(SoundType.METAL),
    ),
    EntityBlock,
    BlockUIMenuType.BlockUI {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity = EssenceConverterBlockEntity(pos, state)

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

    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack,
    ) {
        super.setPlacedBy(level, pos, state, placer, stack)
        level.applyConfigurationCardOnPlacement(pos, placer as? Player)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        if (level.isClientSide || type !== EssenceConverterRegistries.blockEntity.get()) return null
        return BlockEntityTicker { _, _, _, blockEntity ->
            (blockEntity as EssenceConverterBlockEntity).onServerTick()
        }
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean,
    ) {
        if (!level.isClientSide && state.block !== newState.block) {
            val blockEntity = level.blockEntityOrNull(pos, EssenceConverterRegistries.blockEntity.get())
            if (blockEntity != null) {
                val dropped = ItemStack(EssenceConverterRegistries.item.get())
                if (blockEntity.hasContents() || !blockEntity.ioController.configuration.isDefault) {
                    blockEntity.saveToItem(dropped, level.registryAccess())
                }
                popResource(level, pos, dropped)
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
    ): InteractionResult = level.sidedUseResult(openUi(level, pos, player))

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult,
    ): ItemInteractionResult = ioCardInteraction(stack) ?: level.sidedItemUseResult(openUi(level, pos, player))

    override fun createUI(holder: BlockUIMenuType.BlockUIHolder): ModularUI = EssenceConverterUI.create(holder)

    override fun stillValid(holder: BlockUIMenuType.BlockUIHolder): Boolean {
        val level = holder.player.level()
        val blockEntity = level.blockEntityOrNull(holder.pos, EssenceConverterRegistries.blockEntity.get())
        return level.getBlockState(holder.pos).`is`(this) &&
            blockEntity != null &&
            !blockEntity.isRemoved &&
            holder.player.distanceToSqr(
                holder.pos.x + 0.5,
                holder.pos.y + 0.5,
                holder.pos.z + 0.5,
            ) <= MAX_UI_DISTANCE_SQUARED
    }

    private fun openUi(
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        level.blockEntityOrNull(pos, EssenceConverterRegistries.blockEntity.get()) ?: return false
        if (level.isClientSide) return true
        return (player as? ServerPlayer)?.let { BlockUIMenuType.openUI(it, pos) } == true
    }

    private companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING

        const val MAX_UI_DISTANCE_SQUARED = 64.0
    }
}
