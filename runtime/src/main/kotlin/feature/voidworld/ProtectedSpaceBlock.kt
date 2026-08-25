package rhx.lazy.feature.voidworld

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.TransparentBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState

internal class ProtectedSpaceBlock(
    properties: Properties,
) : Block(properties) {
    override fun onDestroyedByPlayer(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        willHarvest: Boolean,
        fluid: FluidState,
    ): Boolean = false

    override fun canHarvestBlock(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        player: Player,
    ): Boolean = false

    override fun canEntityDestroy(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        entity: Entity,
    ): Boolean = false

    override fun canDropFromExplosion(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        explosion: Explosion,
    ): Boolean = false
}

/** Glass-compatible shell face with the same destruction protection as the solid frame. */
internal class ProtectedSpaceWallBlock(
    properties: Properties,
) : TransparentBlock(properties) {
    override fun onDestroyedByPlayer(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        willHarvest: Boolean,
        fluid: FluidState,
    ): Boolean = false

    override fun canHarvestBlock(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        player: Player,
    ): Boolean = false

    override fun canEntityDestroy(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        entity: Entity,
    ): Boolean = false

    override fun canDropFromExplosion(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        explosion: Explosion,
    ): Boolean = false
}
