package rhx.lazy.feature.simulation

import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationJobTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `job defensively snapshots target tools and multipliers`() {
        val target = ItemStack(Items.ZOMBIE_SPAWN_EGG)
        val weapon = ItemStack(Items.DIAMOND_SWORD)
        val job =
            SimulationJob(
                target,
                SimulationBatch.Item(listOf(SimulationItemOutput(ItemStack(Items.ROTTEN_FLESH))), emptyList(), emptyList(), 12),
                duration = 100,
                speedMultiplier = 4,
                outputMultiplier = 12,
                tools = listOf(weapon, ItemStack.EMPTY, ItemStack.EMPTY),
                progressTicks = 25,
            )
        target.count = 0
        weapon.count = 0

        assertTrue(job.target.`is`(Items.ZOMBIE_SPAWN_EGG))
        assertTrue(job.tools.first().`is`(Items.DIAMOND_SWORD))
        assertEquals(12L, job.outputMultiplier)

        val restored = requireNotNull(SimulationJob.parse(registries, job.save(registries)))
        assertEquals(25, restored.progressTicks)
        assertEquals(100, restored.duration)
        assertEquals(4, restored.speedMultiplier)
        assertEquals(12L, restored.outputMultiplier)
        assertTrue(restored.target.`is`(Items.ZOMBIE_SPAWN_EGG))
    }
}
