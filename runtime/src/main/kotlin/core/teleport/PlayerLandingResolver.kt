package rhx.lazy.core.teleport

import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.vehicle.DismountHelper
import net.minecraft.world.level.CollisionGetter
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Resolves a dry, non-hazardous landing position using the player's real collision dimensions.
 */
internal object PlayerLandingResolver {
    fun find(
        level: CollisionGetter,
        pos: BlockPos,
    ): Vec3? {
        if (pos.y < level.minBuildHeight || pos.y >= level.maxBuildHeight) return null

        val stateAtPos = level.getBlockState(pos)
        val stateBelow = level.getBlockState(pos.below())
        if (isUnsafe(stateAtPos) || isUnsafe(stateBelow) || !stateBelow.fluidState.isEmpty) return null

        val position =
            DismountHelper.findSafeDismountLocation(
                EntityType.PLAYER,
                level,
                pos,
                true,
            ) ?: return null

        if (position.y + EntityType.PLAYER.dimensions.height > level.maxBuildHeight) return null
        return position.takeIf { hasAllowedBodySpace(level, it) }
    }

    fun isUnsafe(state: BlockState): Boolean =
        state.`is`(BlockTags.PORTALS) ||
            state.`is`(BlockTags.INVALID_SPAWN_INSIDE) ||
            state.`is`(Blocks.COBWEB) ||
            EntityType.PLAYER.isBlockDangerous(state)

    private fun hasAllowedBodySpace(
        level: CollisionGetter,
        position: Vec3,
    ): Boolean {
        val bodyBox = EntityType.PLAYER.dimensions.makeBoundingBox(position)
        val minX = floor(bodyBox.minX).toInt()
        val maxX = ceil(bodyBox.maxX).toInt() - 1
        val minY = floor(bodyBox.minY).toInt()
        val maxY = ceil(bodyBox.maxY).toInt() - 1
        val minZ = floor(bodyBox.minZ).toInt()
        val maxZ = ceil(bodyBox.maxZ).toInt() - 1

        val mutablePos = BlockPos.MutableBlockPos()
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    mutablePos.set(x, y, z)
                    val state = level.getBlockState(mutablePos)
                    if (!state.fluidState.isEmpty || isUnsafe(state)) return false
                }
            }
        }
        return true
    }
}
