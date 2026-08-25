package rhx.lazy.feature.voidworld

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import rhx.lazy.core.testing.jsonRoundTrip
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncapsulatedSpaceTest {
    private val space =
        EncapsulatedSpace(
            id = UUID.fromString("12345678-1234-5678-9abc-def012345678"),
            ownerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            customName = null,
            allocationOrdinal = 19,
            anchorChunk = ChunkPos(2_048, -2_048),
            status = EncapsulatedSpaceStatus.ACTIVE,
        )

    @Test
    fun `15 cubed interior fits exactly inside anchor chunk`() {
        assertEquals(15, space.innerMax.x - space.innerMin.x + 1)
        assertEquals(15, space.innerMax.y - space.innerMin.y + 1)
        assertEquals(15, space.innerMax.z - space.innerMin.z + 1)
        assertEquals(space.anchorChunk, ChunkPos(space.innerMin))
        assertEquals(space.anchorChunk, ChunkPos(space.innerMax))
        assertEquals(17, space.outerMax.x - space.outerMin.x + 1)
        assertEquals(17, space.outerMax.y - space.outerMin.y + 1)
        assertEquals(17, space.outerMax.z - space.outerMin.z + 1)
    }

    @Test
    fun `one boundary plane is panel and intersections are frame`() {
        assertEquals(
            EncapsulatedShellPart.PANEL,
            EncapsulatedSpaceService.shellPartAt(space, BlockPos(space.outerMin.x, 70, space.outerMin.z + 8)),
        )
        assertEquals(
            EncapsulatedShellPart.FRAME,
            EncapsulatedSpaceService.shellPartAt(space, BlockPos(space.outerMin.x, space.outerMin.y, space.outerMin.z + 8)),
        )
        assertEquals(EncapsulatedShellPart.FRAME, EncapsulatedSpaceService.shellPartAt(space, space.outerMin))
        assertEquals(EncapsulatedShellPart.AIR, EncapsulatedSpaceService.shellPartAt(space, space.spawnPos))
    }

    @Test
    fun `space and saved data snapshot survive codec round trips`() {
        assertEquals(space, EncapsulatedSpace.CODEC.jsonRoundTrip(space))
        val snapshot = EncapsulatedSpaceSnapshot(20, true, listOf(space))
        assertEquals(snapshot, EncapsulatedSpaceSnapshot.CODEC.jsonRoundTrip(snapshot))
    }

    @Test
    fun `default identity and rename validation follow UI rules`() {
        assertEquals("12345678", space.shortId)
        assertNull(EncapsulatedSpaceService.normalizeName("  "))
        assertNull(EncapsulatedSpaceService.normalizeName("bad\u0000name"))
        assertNull(EncapsulatedSpaceService.normalizeName("😀".repeat(33)))
        assertEquals("同名可用", EncapsulatedSpaceService.normalizeName("  同名可用  "))
        assertTrue(
            space
                .displayName()
                .contents
                .toString()
                .contains(EncapsulatedSpace.DEFAULT_NAME_KEY),
        )
    }

    @Test
    fun `owner and operator policies govern management and limits`() {
        val stranger = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff")
        assertTrue(EncapsulatedSpaceService.canManage(space.ownerId, space.ownerId, false))
        assertEquals(false, EncapsulatedSpaceService.canManage(space.ownerId, stranger, false))
        assertTrue(EncapsulatedSpaceService.canManage(space.ownerId, stranger, true))
        assertTrue(EncapsulatedSpaceService.canCreateSpace(63, 64, false))
        assertEquals(false, EncapsulatedSpaceService.canCreateSpace(64, 64, false))
        assertTrue(EncapsulatedSpaceService.canCreateSpace(64, 64, true))
    }

    @Test
    fun `adjacent wall blocks cull their shared translucent face like vanilla glass`() {
        val state = VoidWorldRegistries.spaceWall.get().defaultBlockState()

        assertTrue(state.skipRendering(state, Direction.NORTH))
        assertEquals(false, state.skipRendering(Blocks.GLASS.defaultBlockState(), Direction.NORTH))
    }
}
