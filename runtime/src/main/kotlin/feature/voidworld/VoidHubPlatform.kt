package rhx.lazy.feature.voidworld

import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

internal object VoidHubPlatform {
    const val PLATFORM_Y = 64
    const val STANDING_Y = PLATFORM_Y + 1
    private const val RADIUS = 2

    val standingPos: BlockPos = BlockPos(0, STANDING_Y, 0)

    fun ensureCreated(server: MinecraftServer): Boolean {
        val state = EncapsulatedSpaceState.get(server)
        if (state.isHubCreated()) return true
        val level = server.getLevel(VoidWorldKeys.voidLevel) ?: return false

        for (x in -RADIUS..RADIUS) {
            for (z in -RADIUS..RADIUS) {
                level.setBlock(BlockPos(x, PLATFORM_Y, z), platformStateAt(x, z), Block.UPDATE_ALL)
            }
        }
        state.markHubCreated()
        return true
    }

    fun isHubArea(pos: BlockPos): Boolean =
        pos.x in -RADIUS..RADIUS &&
            pos.z in -RADIUS..RADIUS &&
            pos.y in PLATFORM_Y..(STANDING_Y + 2)

    internal fun platformStateAt(
        x: Int,
        z: Int,
    ): BlockState =
        if (kotlin.math.abs(x) == RADIUS || kotlin.math.abs(z) == RADIUS) {
            Blocks.STONE_BRICKS.defaultBlockState()
        } else {
            Blocks.SMOOTH_STONE.defaultBlockState()
        }
}
