package rhx.lazy.feature.teleporter

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import rhx.lazy.core.teleport.PlayerLandingResolver

/** Deterministically resolves a nearby safe landing without modifying the destination world. */
internal class TeleporterDestinationResolver(
    private val horizontalRadius: Int,
) {
    init {
        TeleporterSearch.validateRadius(horizontalRadius)
    }

    fun resolve(
        level: ServerLevel,
        requested: SavedLocation,
    ): ResolvedDestination? {
        val radiusSquared = horizontalRadius * horizontalRadius
        val candidate = BlockPos.MutableBlockPos()
        for (offset in TeleporterSearch.HORIZONTAL_OFFSETS) {
            if (offset.distanceSquared > radiusSquared) continue
            for (verticalOffset in TeleporterSearch.VERTICAL_OFFSETS) {
                candidate.set(
                    requested.pos.x + offset.x,
                    requested.pos.y + verticalOffset,
                    requested.pos.z + offset.z,
                )
                val position = PlayerLandingResolver.find(level, candidate) ?: continue
                return ResolvedDestination(
                    location = requested.copy(pos = candidate.immutable()),
                    position = position,
                )
            }
        }
        return null
    }
}

internal data class ResolvedDestination(
    val location: SavedLocation,
    val position: Vec3,
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

    internal fun coordinates(
        origin: BlockPos,
        radius: Int,
    ): List<BlockPos> {
        validateRadius(radius)
        val radiusSquared = radius * radius
        return buildList {
            for (offset in HORIZONTAL_OFFSETS) {
                if (offset.distanceSquared > radiusSquared) continue
                for (verticalOffset in VERTICAL_OFFSETS) {
                    add(origin.offset(offset.x, verticalOffset, offset.z))
                }
            }
        }
    }
}
