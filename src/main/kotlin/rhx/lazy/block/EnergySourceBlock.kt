package rhx.lazy.block

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
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
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import rhx.lazy.block.entity.EnergySourceBlockEntity
import rhx.lazy.registry.ModBlockEntities
import rhx.lazy.util.serverTicker

internal class EnergySourceBlock :
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
    ): BlockEntity = EnergySourceBlockEntity(pos, state)

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

    internal fun handleUse(
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        if (!player.isShiftKeyDown || player.isSpectator) return false
        val blockEntity = level.getBlockEntity(pos) as? EnergySourceBlockEntity ?: return false
        if (level.isClientSide) return true

        val enabled = blockEntity.toggleActivePush()
        val stateKey =
            if (enabled) {
                "message.lazy.energy_source.active_push.enabled"
            } else {
                "message.lazy.energy_source.active_push.disabled"
            }
        player.displayClientMessage(
            Component.translatable(
                "message.lazy.energy_source.active_push",
                Component.translatable(stateKey),
            ),
            true,
        )
        return true
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = level.serverTicker(blockEntityType, ModBlockEntities.energySource.get()) { onServerTick() }
}
