package rhx.lazy.block

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import rhx.lazy.block.entity.BufferBlockEntity
import rhx.lazy.menu.BufferMenu
import rhx.lazy.registry.ModItems

internal class BufferBlock :
    Block(
        Properties
            .of()
            .strength(3.5f)
            .sound(SoundType.METAL),
    ),
    EntityBlock {
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
            val blockEntity = level.getBlockEntity(pos) as? BufferBlockEntity
            if (blockEntity != null) {
                val dropped = ItemStack(ModItems.buffer.get())
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
    ): InteractionResult =
        if (handleUse(level, pos, player)) {
            InteractionResult.sidedSuccess(level.isClientSide)
        } else {
            InteractionResult.PASS
        }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult,
    ): ItemInteractionResult =
        if (handleUse(level, pos, player)) {
            ItemInteractionResult.sidedSuccess(level.isClientSide)
        } else {
            ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }

    private fun handleUse(
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        val blockEntity = level.getBlockEntity(pos) as? BufferBlockEntity ?: return false
        if (level.isClientSide) return true

        if (player.isShiftKeyDown) {
            player.displayClientMessage(
                Component.translatable(
                    "message.lazy.buffer.status",
                    blockEntity.totalItemCount,
                    BufferBlockEntity.TOTAL_ITEM_CAPACITY,
                    blockEntity.totalFluidAmount,
                    BufferBlockEntity.TOTAL_FLUID_CAPACITY,
                ),
                true,
            )
            return true
        }

        val serverPlayer = player as? ServerPlayer ?: return false
        val provider =
            SimpleMenuProvider(
                { containerId, inventory, _ ->
                    BufferMenu.createServer(containerId, inventory, blockEntity, serverPlayer)
                },
                blockEntity.blockState.block.name,
            )
        serverPlayer.openMenu(provider) { buffer -> blockEntity.snapshot().write(buffer) }
        return true
    }
}
