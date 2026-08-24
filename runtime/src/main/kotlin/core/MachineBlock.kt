package rhx.lazy.core

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
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
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.items.ItemHandlerHelper
import rhx.lazy.core.io.applyConfigurationCardOnPlacement
import rhx.lazy.core.io.ioCardInteraction
import rhx.lazy.integration.api.LazyInternalApi

/**
 * Shared behaviour for Lazy's machines: horizontal facing, configuration-card seeding on placement,
 * one screen-opening path, and a single drop pipeline that both breaking and wrenching go through.
 *
 * Machines still bring their own block entity, screen and ticker; everything a machine does not have
 * an opinion about lives here.
 */
@LazyInternalApi
public abstract class MachineBlock(
    properties: Properties,
) : Block(properties),
    EntityBlock,
    BlockUIMenuType.BlockUI {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

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

    /**
     * The one place a machine turns into items. A machine removed without its block entity — the
     * wrench takes that entity away first — has already handed everything over and leaves nothing.
     */
    final override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean,
    ) {
        if (!level.isClientSide && state.block !== newState.block) {
            machineBlockEntity(level, pos)?.let { blockEntity ->
                takeDrops(level, blockEntity).forEach { drop -> popResource(level, pos, drop) }
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
    ): ItemInteractionResult = ioCardInteraction(stack) ?: level.sidedItemUseResult(handleUse(level, pos, player))

    override fun stillValid(holder: BlockUIMenuType.BlockUIHolder): Boolean {
        val level = holder.player.level()
        val blockEntity = level.getBlockEntity(holder.pos)
        return level.getBlockState(holder.pos).`is`(this) &&
            blockEntity != null &&
            !blockEntity.isRemoved &&
            holder.player.distanceToSqr(holder.pos.center) <= MAX_UI_DISTANCE_SQUARED
    }

    /** Opens the machine's screen. Machines with their own bare-handed behaviour override this. */
    protected open fun handleUse(
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        machineBlockEntity(level, pos) ?: return false
        if (level.isClientSide) return true
        val serverPlayer = player as? ServerPlayer ?: return false
        return BlockUIMenuType.openUI(serverPlayer, pos)
    }

    /**
     * Takes the machine apart for [player]: the drops go to their inventory, and only what does not
     * fit lands at their feet. Dropping the block entity first is what keeps [onRemove] from handing
     * out a second copy.
     */
    public fun dismantle(
        level: ServerLevel,
        pos: BlockPos,
        player: Player,
    ) {
        val blockEntity = machineBlockEntity(level, pos)
        val drops = blockEntity?.let { takeDrops(level, it) } ?: listOf(ItemStack(this))
        if (blockEntity != null) level.removeBlockEntity(pos)
        level.destroyBlock(pos, false, player)
        drops.forEach { drop -> ItemHandlerHelper.giveItemToPlayer(player, drop) }
    }

    /**
     * Turns the machine one step clockwise. The side configuration follows the facing, so the
     * capability cache is dropped for neighbours that resolved a face before the turn.
     */
    public fun rotateClockwise(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
    ) {
        level.setBlockAndUpdate(pos, rotate(state, Rotation.CLOCKWISE_90))
        level.getBlockEntity(pos)?.invalidateCapabilities()
        level.playSound(null, pos, state.soundType.placeSound, SoundSource.BLOCKS, 0.6f, 1.2f)
    }

    /** The machine itself first, then anything it was merely holding. */
    private fun takeDrops(
        level: Level,
        blockEntity: MachineBlockEntity,
    ): List<ItemStack> {
        val machine = ItemStack(this)
        if (blockEntity.hasStoredContents()) blockEntity.saveContentsToItem(machine, level.registryAccess())
        return listOf(machine) + blockEntity.takeHeldItems()
    }

    private fun machineBlockEntity(
        level: Level,
        pos: BlockPos,
    ): MachineBlockEntity? = level.getBlockEntity(pos) as? MachineBlockEntity

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING

        private const val MAX_UI_DISTANCE_SQUARED = 64.0
    }
}
