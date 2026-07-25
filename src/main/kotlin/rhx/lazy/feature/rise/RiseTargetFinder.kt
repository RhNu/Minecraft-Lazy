package rhx.lazy.feature.rise

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel

internal object RiseTargetFinder {
    /**
     * Finds the first support block above [start] with two clear blocks above it and sky visibility.
     * The scan is bounded by the world's build height, so command execution cannot run forever.
     */
    fun find(
        level: ServerLevel,
        start: BlockPos,
    ): BlockPos? {
        val firstY = start.y.coerceAtLeast(level.minBuildHeight)
        val lastY = level.maxBuildHeight - 3
        if (firstY > lastY) return null

        for (y in firstY..lastY) {
            val candidate = BlockPos(start.x, y, start.z)
            if (isValid(level, candidate)) return candidate
        }
        return null
    }

    private fun isValid(
        level: ServerLevel,
        support: BlockPos,
    ): Boolean {
        if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return false

        val feet = support.above()
        return level.isEmptyBlock(feet) &&
            level.isEmptyBlock(feet.above()) &&
            level.canSeeSky(feet)
    }
}
