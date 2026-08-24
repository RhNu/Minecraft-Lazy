package rhx.lazy.core.teleport

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.CollisionGetter
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerLandingResolverTest {
    @Test
    fun `collision-free vegetation does not block a landing`() {
        val feet = BlockPos(0, 65, 0)
        val level =
            TestCollisionLevel(
                mapOf(
                    feet.below() to Blocks.GRASS_BLOCK.defaultBlockState(),
                    feet to Blocks.SHORT_GRASS.defaultBlockState(),
                ),
            )

        assertEquals(Vec3.atBottomCenterOf(feet), PlayerLandingResolver.find(level, feet))
    }

    @Test
    fun `single snow layer does not block a landing`() {
        val feet = BlockPos(0, 65, 0)
        val level =
            TestCollisionLevel(
                mapOf(
                    feet.below() to Blocks.GRASS_BLOCK.defaultBlockState(),
                    feet to Blocks.SNOW.defaultBlockState(),
                ),
            )

        assertEquals(Vec3.atBottomCenterOf(feet), PlayerLandingResolver.find(level, feet))
    }

    @Test
    fun `bottom slab contributes its real landing height`() {
        val slab = BlockPos(0, 65, 0)
        val level = TestCollisionLevel(mapOf(slab to Blocks.STONE_SLAB.defaultBlockState()))

        assertEquals(Vec3.atBottomCenterOf(slab).add(0.0, 0.5, 0.0), PlayerLandingResolver.find(level, slab))
    }

    @Test
    fun `liquid body space is rejected`() {
        val feet = BlockPos(0, 65, 0)
        val level =
            TestCollisionLevel(
                mapOf(
                    feet.below() to Blocks.STONE.defaultBlockState(),
                    feet to Blocks.WATER.defaultBlockState(),
                ),
            )

        assertNull(PlayerLandingResolver.find(level, feet))
    }

    @Test
    fun `dangerous floor is rejected`() {
        val feet = BlockPos(0, 65, 0)
        val level = TestCollisionLevel(mapOf(feet.below() to Blocks.MAGMA_BLOCK.defaultBlockState()))

        assertNull(PlayerLandingResolver.find(level, feet))
    }

    private class TestCollisionLevel(
        blocks: Map<BlockPos, BlockState>,
    ) : CollisionGetter {
        private val blocks = blocks.mapKeys { (pos, _) -> pos.immutable() }
        private val worldBorder = WorldBorder()

        override fun getBlockEntity(pos: BlockPos): BlockEntity? = null

        override fun getBlockState(pos: BlockPos): BlockState = blocks[pos] ?: Blocks.AIR.defaultBlockState()

        override fun getFluidState(pos: BlockPos): FluidState = getBlockState(pos).fluidState

        override fun getHeight(): Int = 384

        override fun getMinBuildHeight(): Int = -64

        override fun getWorldBorder(): WorldBorder = worldBorder

        override fun getChunkForCollisions(
            chunkX: Int,
            chunkZ: Int,
        ): BlockGetter = this

        override fun getEntityCollisions(
            entity: Entity?,
            collisionBox: AABB,
        ): List<VoxelShape> = emptyList()
    }
}
