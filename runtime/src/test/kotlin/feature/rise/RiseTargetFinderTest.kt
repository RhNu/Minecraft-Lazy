package rhx.lazy.feature.rise

import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RiseTargetFinderTest {
    @Test
    fun `search skips safe landings without sky visibility`() {
        val hidden = Vec3(4.5, 20.0, -2.5)
        val visible = Vec3(4.5, 22.125, -2.5)

        val result =
            RiseTargetFinder.findInColumn(
                x = 4,
                z = -3,
                firstY = 20,
                lastY = 22,
                resolveLanding = { pos ->
                    when (pos.y) {
                        20 -> hidden
                        22 -> visible
                        else -> null
                    }
                },
                canSeeSky = { pos -> pos.y == 23 },
            )

        assertEquals(visible, result)
    }

    @Test
    fun `search preserves the precise landing height`() {
        val landing = Vec3(0.5, 65.5, 0.5)

        val result =
            RiseTargetFinder.findInColumn(
                x = 0,
                z = 0,
                firstY = 65,
                lastY = 65,
                resolveLanding = { landing },
                canSeeSky = { true },
            )

        assertEquals(landing, result)
    }

    @Test
    fun `invalid scan range has no target`() {
        assertNull(
            RiseTargetFinder.findInColumn(
                x = 0,
                z = 0,
                firstY = 10,
                lastY = 9,
                resolveLanding = { error("resolver must not be called") },
                canSeeSky = { error("sky check must not be called") },
            ),
        )
    }
}
