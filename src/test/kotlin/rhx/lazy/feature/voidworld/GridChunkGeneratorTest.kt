package rhx.lazy.feature.voidworld

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GridChunkGeneratorTest {
    @Test
    fun `detects borders across positive and negative coordinates`() {
        val size = 48
        assertTrue(GridChunkGenerator.isBorderCoordinate(0, 20, size))
        assertTrue(GridChunkGenerator.isBorderCoordinate(47, 20, size))
        assertTrue(GridChunkGenerator.isBorderCoordinate(-1, 20, size))
        assertTrue(GridChunkGenerator.isBorderCoordinate(-48, 20, size))
        assertTrue(GridChunkGenerator.isBorderCoordinate(20, 48, size))
        assertTrue(GridChunkGenerator.isBorderCoordinate(20, -49, size))
        assertFalse(GridChunkGenerator.isBorderCoordinate(1, 1, size))
        assertFalse(GridChunkGenerator.isBorderCoordinate(46, 46, size))
        assertFalse(GridChunkGenerator.isBorderCoordinate(-2, -2, size))
    }

    @Test
    fun `rejects a non-positive grid size`() {
        var rejected = false
        try {
            GridChunkGenerator.isBorderCoordinate(0, 0, 0)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun `base height follows the requested heightmap predicate`() {
        val minBuildHeight = -64
        val layerHeight = 128

        assertEquals(
            minBuildHeight,
            GridChunkGenerator.baseHeightFor(
                Blocks.AIR.defaultBlockState(),
                Heightmap.Types.WORLD_SURFACE,
                minBuildHeight,
                layerHeight,
            ),
        )
        assertEquals(
            minBuildHeight,
            GridChunkGenerator.baseHeightFor(
                Blocks.OAK_LEAVES.defaultBlockState(),
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                minBuildHeight,
                layerHeight,
            ),
        )
        assertEquals(
            layerHeight + 1,
            GridChunkGenerator.baseHeightFor(
                Blocks.STONE.defaultBlockState(),
                Heightmap.Types.MOTION_BLOCKING,
                minBuildHeight,
                layerHeight,
            ),
        )
    }
}
