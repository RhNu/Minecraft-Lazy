package rhx.lazy.feature.teleporter

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import rhx.lazy.core.teleport.PlayerLandingResolver

/**
 * Finds a player-sized, vanilla-compatible landing position and optionally builds a transactional
 * safety platform in the void dimension.
 */
internal class TeleporterDestinationResolver(
    private val horizontalRadius: Int,
    private val createVoidSafetyPlatform: Boolean,
) {
    init {
        TeleporterSearch.validateRadius(horizontalRadius)
    }

    fun resolve(
        level: ServerLevel,
        requested: SavedLocation,
        allowPlatform: Boolean,
    ): ResolvedDestination? {
        findNearby(level, requested.pos, ::findSafePosition)?.let { candidate ->
            return candidate.toResolvedDestination(requested)
        }

        if (!allowPlatform || !createVoidSafetyPlatform) return null

        val clearCandidate =
            findNearby(level, requested.pos) { _, pos ->
                if (hasClearBodySpace(level, pos)) {
                    Vec3.atBottomCenterOf(pos)
                } else {
                    null
                }
            } ?: return null

        val mutation = createSafetyPlatform(level, clearCandidate.anchor)
        val safePosition = findSafePosition(level, clearCandidate.anchor)
        if (safePosition == null) {
            mutation.rollback()
            return null
        }

        return ResolvedDestination(
            location = requested.copy(pos = clearCandidate.anchor),
            position = safePosition,
            platformMutation = mutation,
        )
    }

    private fun findNearby(
        level: ServerLevel,
        origin: BlockPos,
        predicate: (ServerLevel, BlockPos) -> Vec3?,
    ): SafeCandidate? {
        val radiusSquared = horizontalRadius * horizontalRadius
        val candidate = BlockPos.MutableBlockPos()

        // Horizontal distance is the primary ordering. For each column, check the current level
        // first and then the closest levels above/below it. This keeps common slab/stair cases
        // cheap while still bounding the worst-case search.
        for (offset in TeleporterSearch.HORIZONTAL_OFFSETS) {
            if (offset.distanceSquared > radiusSquared) continue
            for (verticalOffset in TeleporterSearch.VERTICAL_OFFSETS) {
                candidate.set(
                    origin.x + offset.x,
                    origin.y + verticalOffset,
                    origin.z + offset.z,
                )
                val position = predicate(level, candidate) ?: continue
                return SafeCandidate(candidate.immutable(), position)
            }
        }
        return null
    }

    private fun findSafePosition(
        level: ServerLevel,
        pos: BlockPos,
    ): Vec3? = PlayerLandingResolver.find(level, pos)

    private fun hasClearBodySpace(
        level: ServerLevel,
        pos: BlockPos,
    ): Boolean {
        if (!isCandidateWithinBuildHeight(level, pos)) return false

        val stateAtPos = level.getBlockState(pos)
        val abovePos = pos.above()
        val stateAbove = level.getBlockState(abovePos)
        if (PlayerLandingResolver.isUnsafe(stateAtPos) || PlayerLandingResolver.isUnsafe(stateAbove)) return false
        if (!stateAtPos.fluidState.isEmpty || !stateAbove.fluidState.isEmpty) return false
        if (!stateAtPos.getCollisionShape(level, pos).isEmpty || !stateAbove.getCollisionShape(level, abovePos).isEmpty) {
            return false
        }

        val bodyBox = EntityType.PLAYER.dimensions.makeBoundingBox(Vec3.atBottomCenterOf(pos))
        return level.worldBorder.isWithinBounds(bodyBox) &&
            level.getBlockCollisions(null, bodyBox).none { !it.isEmpty }
    }

    private fun isCandidateWithinBuildHeight(
        level: ServerLevel,
        pos: BlockPos,
    ): Boolean =
        pos.y > level.minBuildHeight &&
            pos.y < level.maxBuildHeight &&
            pos.y + 1 < level.maxBuildHeight

    private fun createSafetyPlatform(
        level: ServerLevel,
        standingPos: BlockPos,
    ): PlatformMutation {
        val platformState = Blocks.GLASS.defaultBlockState()
        val platformY = standingPos.y - 1
        val changedBlocks = ArrayList<ChangedBlock>(9)

        for (xOffset in -1..1) {
            for (zOffset in -1..1) {
                val platformPos =
                    BlockPos(
                        standingPos.x + xOffset,
                        platformY,
                        standingPos.z + zOffset,
                    )
                val previousState = level.getBlockState(platformPos)
                if (previousState.canBeReplaced() && level.setBlockAndUpdate(platformPos, platformState)) {
                    changedBlocks.add(ChangedBlock(platformPos, previousState, platformState))
                }
            }
        }
        return PlatformMutation(level, changedBlocks)
    }

    private data class SafeCandidate(
        val anchor: BlockPos,
        val position: Vec3,
    ) {
        fun toResolvedDestination(requested: SavedLocation): ResolvedDestination =
            ResolvedDestination(
                location = requested.copy(pos = anchor),
                position = position,
            )
    }
}

internal data class ResolvedDestination(
    val location: SavedLocation,
    val position: Vec3,
    val platformMutation: PlatformMutation? = null,
)

internal class PlatformMutation(
    private val level: ServerLevel,
    private val changedBlocks: List<ChangedBlock>,
) {
    private var rolledBack = false

    fun rollback() {
        if (rolledBack) return
        rolledBack = true
        changedBlocks.asReversed().forEach { change ->
            if (level.getBlockState(change.pos) == change.placedState) {
                level.setBlockAndUpdate(change.pos, change.previousState)
            }
        }
    }
}

internal data class ChangedBlock(
    val pos: BlockPos,
    val previousState: BlockState,
    val placedState: BlockState,
)

internal data class TeleporterHorizontalOffset(
    val x: Int,
    val z: Int,
    val distanceSquared: Int,
)

internal object TeleporterSearch {
    const val MAX_HORIZONTAL_RADIUS = 16

    val VERTICAL_OFFSETS: IntArray = intArrayOf(0, 1, -1, 2, -2, 3, -3, 4, -4)

    val HORIZONTAL_OFFSETS: Array<TeleporterHorizontalOffset> =
        buildList {
            for (x in -MAX_HORIZONTAL_RADIUS..MAX_HORIZONTAL_RADIUS) {
                for (z in -MAX_HORIZONTAL_RADIUS..MAX_HORIZONTAL_RADIUS) {
                    val distanceSquared = x * x + z * z
                    if (distanceSquared <= MAX_HORIZONTAL_RADIUS * MAX_HORIZONTAL_RADIUS) {
                        add(TeleporterHorizontalOffset(x, z, distanceSquared))
                    }
                }
            }
        }.sortedWith(
            compareBy<TeleporterHorizontalOffset> { it.distanceSquared }
                .thenBy { it.x }
                .thenBy { it.z },
        ).toTypedArray()

    fun validateRadius(radius: Int) {
        require(radius in 0..MAX_HORIZONTAL_RADIUS) {
            "safe search radius must be between 0 and $MAX_HORIZONTAL_RADIUS"
        }
    }

    /**
     * Pure search-order helper used by unit tests and diagnostics; gameplay uses the
     * allocation-free loop above.
     */
    internal fun coordinates(
        origin: BlockPos,
        radius: Int,
    ): List<BlockPos> {
        validateRadius(radius)
        val radiusSquared = radius * radius
        val coordinates = ArrayList<BlockPos>()
        for (offset in HORIZONTAL_OFFSETS) {
            if (offset.distanceSquared > radiusSquared) continue
            for (verticalOffset in VERTICAL_OFFSETS) {
                coordinates.add(
                    BlockPos(
                        origin.x + offset.x,
                        origin.y + verticalOffset,
                        origin.z + offset.z,
                    ),
                )
            }
        }
        return coordinates
    }
}
