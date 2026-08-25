package rhx.lazy.feature.voidworld

import net.minecraft.world.level.ChunkPos
import kotlin.math.roundToInt

internal object EncapsulatedSpaceAllocator {
    const val GRID_BLOCKS = 1_024
    const val GRID_CHUNKS = GRID_BLOCKS / 16
    const val FIRST_RING = 32
    const val CENTER_CLEARANCE = GRID_BLOCKS * FIRST_RING

    fun anchorFor(ordinal: Long): ChunkPos {
        require(ordinal >= 0) { "allocation ordinal must be non-negative" }

        var ring = FIRST_RING
        var remaining = ordinal
        while (remaining >= slotsIn(ring)) {
            remaining -= slotsIn(ring)
            ring++
        }

        val slotCount = slotsIn(ring)
        val stride = coprimeStride(slotCount)
        val perimeterIndex = ((remaining % slotCount) * stride % slotCount).toInt()
        val grid = perimeterCoordinate(ring, perimeterIndex)
        return ChunkPos(grid.first * GRID_CHUNKS, grid.second * GRID_CHUNKS)
    }

    internal fun slotsIn(ring: Int): Long {
        require(ring > 0)
        return ring.toLong() * 8L
    }

    internal fun coprimeStride(slotCount: Long): Long {
        var stride = (slotCount / GOLDEN_RATIO).roundToInt().toLong().coerceAtLeast(1)
        while (greatestCommonDivisor(stride, slotCount) != 1L) {
            stride--
        }
        return stride
    }

    internal fun perimeterCoordinate(
        ring: Int,
        index: Int,
    ): Pair<Int, Int> {
        val side = ring * 2
        require(index in 0 until side * 4)
        return when {
            index < side -> -ring + index to -ring
            index < side * 2 -> ring to -ring + (index - side)
            index < side * 3 -> ring - (index - side * 2) to ring
            else -> -ring to ring - (index - side * 3)
        }
    }

    private tailrec fun greatestCommonDivisor(
        left: Long,
        right: Long,
    ): Long = if (right == 0L) kotlin.math.abs(left) else greatestCommonDivisor(right, left % right)

    private const val GOLDEN_RATIO = 1.618033988749895
}
