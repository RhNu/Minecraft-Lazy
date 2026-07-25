package rhx.lazy.feature.energy

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
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.serverTicker
import rhx.lazy.core.sidedItemUseResult
import rhx.lazy.core.sidedUseResult

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

    internal fun handleUse(
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        if (!player.isShiftKeyDown || player.isSpectator) return false
        val blockEntity = level.blockEntityOrNull(pos, EnergyRegistries.sourceBlockEntity.get()) ?: return false
        if (level.isClientSide) return true

        val enabled = blockEntity.toggleActivePush()
        val stateKey =
            if (enabled) {
                "message.lazy.energy_source.active_push.enabled"
            } else {
                "message.lazy.energy_source.active_push.disabled"
            }
        player.displayActionBar(
            "message.lazy.energy_source.active_push",
            Component.translatable(stateKey),
        )
        return true
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = level.serverTicker(blockEntityType, EnergyRegistries.sourceBlockEntity.get()) { onServerTick() }
}
