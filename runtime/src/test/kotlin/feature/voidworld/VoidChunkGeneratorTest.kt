package rhx.lazy.feature.voidworld

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoidChunkGeneratorTest {
    @Test
    fun `platform is five by five and centered at the origin`() {
        assertTrue(VoidChunkGenerator.isPlatformCoordinate(-2, -2))
        assertTrue(VoidChunkGenerator.isPlatformCoordinate(0, 0))
        assertTrue(VoidChunkGenerator.isPlatformCoordinate(2, 2))
        assertFalse(VoidChunkGenerator.isPlatformCoordinate(-3, 0))
        assertFalse(VoidChunkGenerator.isPlatformCoordinate(0, 3))
        assertFalse(VoidChunkGenerator.isPlatformCoordinate(3, 3))
    }

    @Test
    fun `platform keeps the former border and inner block styles`() {
        assertEquals(Blocks.STONE_BRICKS.defaultBlockState(), VoidChunkGenerator.platformStateAt(-2, 0))
        assertEquals(Blocks.SMOOTH_STONE.defaultBlockState(), VoidChunkGenerator.platformStateAt(0, 0))
        assertNull(VoidChunkGenerator.platformStateAt(3, 0))
    }

    @Test
    fun `base height is empty outside the platform`() {
        assertEquals(
            -64,
            VoidChunkGenerator.baseHeightFor(
                Blocks.AIR.defaultBlockState(),
                Heightmap.Types.WORLD_SURFACE,
                -64,
                VoidChunkGenerator.PLATFORM_Y,
            ),
        )
        assertEquals(
            VoidChunkGenerator.PLATFORM_Y + 1,
            VoidChunkGenerator.baseHeightFor(
                Blocks.STONE.defaultBlockState(),
                Heightmap.Types.MOTION_BLOCKING,
                -64,
                VoidChunkGenerator.PLATFORM_Y,
            ),
        )
    }
}
