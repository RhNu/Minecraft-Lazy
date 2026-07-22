package rhx.lazy.item

import net.minecraft.core.BlockPos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeleporterDestinationResolverTest {
    @Test
    fun `search starts at the origin and stays inside the configured radius`() {
        val origin = BlockPos(12, 64, -8)
        val radius = 3
        val coordinates = TeleporterSearch.coordinates(origin, radius)

        assertEquals(origin, coordinates.first())
        assertTrue(
            coordinates.all { pos ->
                val xOffset = pos.x - origin.x
                val zOffset = pos.z - origin.z
                xOffset * xOffset + zOffset * zOffset <= radius * radius
            },
        )
    }

    @Test
    fun `search order contains no duplicate candidates`() {
        val coordinates = TeleporterSearch.coordinates(BlockPos.ZERO, TeleporterSearch.MAX_HORIZONTAL_RADIUS)

        assertEquals(coordinates.size, coordinates.toSet().size)
    }

    @Test
    fun `circular search has fewer worst-case candidates than the old square search`() {
        val radius = TeleporterSearch.MAX_HORIZONTAL_RADIUS
        val coordinates = TeleporterSearch.coordinates(BlockPos.ZERO, radius)
        val oldSquareCandidateCount =
            (radius * 2 + 1) * (radius * 2 + 1) * TeleporterSearch.VERTICAL_OFFSETS.size

        assertTrue(coordinates.size < oldSquareCandidateCount)
    }
}
