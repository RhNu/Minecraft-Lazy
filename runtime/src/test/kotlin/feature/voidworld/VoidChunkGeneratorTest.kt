package rhx.lazy.feature.voidworld

import net.minecraft.world.level.block.Blocks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoidChunkGeneratorTest {
    @Test
    fun `base columns contain only air`() {
        val states = VoidChunkGenerator.emptyColumnStates(384)

        assertEquals(384, states.size)
        assertTrue(states.all { state -> state.isAir })
    }

    @Test
    fun `hub platform is a one-time five by five layout`() {
        val states =
            (-2..2).flatMap { x ->
                (-2..2).map { z -> VoidHubPlatform.platformStateAt(x, z) }
            }

        assertEquals(25, states.size)
        assertEquals(16, states.count { it.`is`(Blocks.STONE_BRICKS) })
        assertEquals(9, states.count { it.`is`(Blocks.SMOOTH_STONE) })
    }
}
