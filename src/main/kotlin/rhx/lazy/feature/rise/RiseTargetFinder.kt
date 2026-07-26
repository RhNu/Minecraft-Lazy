package rhx.lazy.feature.rise

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import rhx.lazy.core.teleport.PlayerLandingResolver

internal object RiseTargetFinder {
    /**
     * Finds the first player-sized landing position above [start] with sky visibility.
     * The scan is bounded by the world's build height, so command execution cannot run forever.
     */
    fun find(
        level: ServerLevel,
        start: BlockPos,
    ): Vec3? {
        val firstY = start.y.coerceAtLeast(level.minBuildHeight)
        val lastY = level.maxBuildHeight - 1
        if (firstY > lastY) return null

        return findInColumn(
            x = start.x,
            z = start.z,
            firstY = firstY,
            lastY = lastY,
            resolveLanding = { pos -> PlayerLandingResolver.find(level, pos) },
            canSeeSky = level::canSeeSky,
        )
    }

    internal fun findInColumn(
        x: Int,
        z: Int,
        firstY: Int,
        lastY: Int,
        resolveLanding: (BlockPos) -> Vec3?,
        canSeeSky: (BlockPos) -> Boolean,
    ): Vec3? {
        if (firstY > lastY) return null

        for (y in firstY..lastY) {
            val landing = resolveLanding(BlockPos(x, y, z)) ?: continue
            val skyCheckPos = BlockPos.containing(landing).above()
            if (canSeeSky(skyCheckPos)) return landing
        }
        return null
    }
}
