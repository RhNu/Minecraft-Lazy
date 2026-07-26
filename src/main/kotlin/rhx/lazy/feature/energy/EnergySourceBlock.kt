package rhx.lazy.feature.energy

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
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
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.serverTicker
import rhx.lazy.core.sidedItemUseResult
import rhx.lazy.core.sidedUseResult
import rhx.lazy.integration.beyonddimensions.BeyondDimensionsIntegration
import rhx.lazy.integration.beyonddimensions.DimensionNetworkResult

internal class EnergySourceBlock :
    Block(
        Properties
            .of()
            .strength(0.5f)
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

        val serverPlayer = player as? ServerPlayer ?: return false
        val primaryNetworkId =
            if (blockEntity.nextModeNeedsNetwork()) {
                when (val result = BeyondDimensionsIntegration.primaryNetwork(serverPlayer)) {
                    is DimensionNetworkResult.Success -> result.value
                    DimensionNetworkResult.NetworkNotFound -> {
                        player.displayActionBar("message.lazy.beyond_dimensions.no_primary_network")
                        return true
                    }

                    else -> {
                        player.displayActionBar("message.lazy.beyond_dimensions.unavailable")
                        return true
                    }
                }
            } else {
                null
            }

        val mode = blockEntity.cycleOutputMode(primaryNetworkId) ?: return true
        player.displayActionBar(
            "message.lazy.energy_source.output_mode",
            Component.translatable(mode.translationKey),
        )
        return true
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = level.serverTicker(blockEntityType, EnergyRegistries.sourceBlockEntity.get()) { onServerTick() }
}

private val EnergyOutputMode.translationKey: String
    get() =
        when (this) {
            EnergyOutputMode.OFF -> "message.lazy.energy_source.output_mode.off"
            EnergyOutputMode.ADJACENT -> "message.lazy.energy_source.output_mode.adjacent"
            EnergyOutputMode.NETWORK -> "message.lazy.energy_source.output_mode.network"
            EnergyOutputMode.BOTH -> "message.lazy.energy_source.output_mode.both"
        }
