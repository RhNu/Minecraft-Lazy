package rhx.lazy.feature.voidworld

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncapsulatedSpaceAllocatorTest {
    @Test
    fun `allocation is deterministic unique and starts outside center clearance`() {
        val firstRun = (0L until 2_048L).map(EncapsulatedSpaceAllocator::anchorFor)
        val secondRun = (0L until 2_048L).map(EncapsulatedSpaceAllocator::anchorFor)

        assertEquals(firstRun, secondRun)
        assertEquals(firstRun.size, firstRun.toSet().size)
        val ids = (0L until 2_048L).map(EncapsulatedSpaceState::spaceIdFor)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(ids, (0L until 2_048L).map(EncapsulatedSpaceState::spaceIdFor))
        assertTrue(
            firstRun.all { anchor ->
                max(abs(anchor.minBlockX), abs(anchor.minBlockZ)) >= EncapsulatedSpaceAllocator.CENTER_CLEARANCE
            },
        )
    }

    @Test
    fun `golden stride distributes early allocations across all quadrants`() {
        val quadrants =
            (0L until 16L)
                .map(EncapsulatedSpaceAllocator::anchorFor)
                .map { anchor -> (anchor.x >= 0) to (anchor.z >= 0) }
                .toSet()

        assertEquals(4, quadrants.size)
    }

    @Test
    fun `nearest outer shells leave at least 1007 empty blocks`() {
        val anchors = (0L until 512L).map(EncapsulatedSpaceAllocator::anchorFor)
        for (leftIndex in anchors.indices) {
            for (rightIndex in leftIndex + 1 until anchors.size) {
                val left = anchors[leftIndex]
                val right = anchors[rightIndex]
                val separation = max(abs(left.minBlockX - right.minBlockX), abs(left.minBlockZ - right.minBlockZ))
                assertTrue(separation - EncapsulatedSpace.OUTER_SIZE >= 1_007)
            }
        }
    }
}
