package rhx.lazy.feature.buffer

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.sidedItemUseResult
import rhx.lazy.core.sidedUseResult

internal class BufferBlock :
    Block(
        Properties
            .of()
            .strength(0.5f)
            .sound(SoundType.METAL),
    ),
    EntityBlock,
    BlockUIMenuType.BlockUI {
    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity = BufferBlockEntity(pos, state)

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean,
    ) {
        if (!level.isClientSide && state.block !== newState.block) {
            val blockEntity = level.blockEntityOrNull(pos, BufferRegistries.blockEntity.get())
            if (blockEntity != null) {
                val dropped = ItemStack(BufferRegistries.item.get())
                if (blockEntity.hasContents()) {
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
    ): InteractionResult = level.sidedUseResult(handleUse(level, pos, player))

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult,
    ): ItemInteractionResult = level.sidedItemUseResult(handleUse(level, pos, player))

    override fun createUI(holder: BlockUIMenuType.BlockUIHolder): ModularUI = BufferUI.create(holder)

    override fun stillValid(holder: BlockUIMenuType.BlockUIHolder): Boolean {
        val level = holder.player.level()
        val blockEntity = level.blockEntityOrNull(holder.pos, BufferRegistries.blockEntity.get())
        return level.getBlockState(holder.pos).`is`(this) &&
            blockEntity != null &&
            !blockEntity.isRemoved &&
            holder.player.distanceToSqr(
                holder.pos.x + 0.5,
                holder.pos.y + 0.5,
                holder.pos.z + 0.5,
            ) <= MAX_UI_DISTANCE_SQUARED
    }

    private fun handleUse(
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        val blockEntity = level.blockEntityOrNull(pos, BufferRegistries.blockEntity.get()) ?: return false
        if (level.isClientSide) return true

        if (player.isShiftKeyDown) {
            player.displayActionBar(
                "message.lazy.buffer.status",
                blockEntity.totalItemCount,
                BufferBlockEntity.TOTAL_ITEM_CAPACITY,
                blockEntity.totalFluidAmount,
                BufferBlockEntity.TOTAL_FLUID_CAPACITY,
            )
            return true
        }

        val serverPlayer = player as? ServerPlayer ?: return false
        return BlockUIMenuType.openUI(serverPlayer, pos)
    }

    private companion object {
        const val MAX_UI_DISTANCE_SQUARED = 64.0
    }
}
