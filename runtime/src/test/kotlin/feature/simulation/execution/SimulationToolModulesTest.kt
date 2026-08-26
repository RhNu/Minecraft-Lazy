package rhx.lazy.feature.simulation

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SimulationToolModulesTest {
    @Test
    fun `the first slot offering a weapon wins`() {
        val loadout =
            SimulationToolModules.buildLoadout(
                listOf(weaponModule),
                listOf(ItemStack.EMPTY, ItemStack(Items.DIAMOND_SWORD), ItemStack(Items.NETHERITE_SWORD)),
            )

        assertTrue(loadout.weapon.`is`(Items.DIAMOND_SWORD))
    }

    @Test
    fun `output processors from different tools contribute to the loadout`() {
        val loadout =
            SimulationToolModules.buildLoadout(
                listOf(rejectModule(Items.DIAMOND_SWORD, Items.BONE), rejectModule(Items.LAVA_BUCKET, Items.ROTTEN_FLESH)),
                listOf(ItemStack(Items.DIAMOND_SWORD), ItemStack(Items.LAVA_BUCKET)),
            )

        assertFalse(loadout === SimulationToolLoadout.EMPTY)
    }

    @Test
    fun `a module only sees the stacks it claims`() {
        val loadout =
            SimulationToolModules.buildLoadout(
                listOf(weaponModule),
                listOf(ItemStack(Items.LAVA_BUCKET), ItemStack(Items.DIAMOND_SWORD)),
            )

        assertTrue(loadout.weapon.`is`(Items.DIAMOND_SWORD))
    }

    @Test
    fun `empty tool slots produce the shared empty loadout`() {
        val loadout = SimulationToolModules.buildLoadout(listOf(weaponModule), List(3) { ItemStack.EMPTY })

        assertSame(SimulationToolLoadout.EMPTY, loadout)
        assertTrue(loadout.weapon.isEmpty)
    }

    @Test
    fun `an empty stack is never claimed`() {
        assertFalse(SimulationToolModules.claims(ItemStack.EMPTY))
    }

    @Test
    fun `a grindstone is a simulation tool`() {
        assertTrue(GrindstoneToolModule.claims(ItemStack(Items.GRINDSTONE)))
    }

    @Test
    fun `output processors use priority before tool-slot order`() {
        val loadout =
            SimulationToolModules.buildLoadout(
                listOf(processorModule(Items.DIAMOND_SWORD, 100), processorModule(Items.LAVA_BUCKET, 0)),
                listOf(ItemStack(Items.DIAMOND_SWORD), ItemStack(Items.LAVA_BUCKET)),
            )

        assertEquals(listOf(0, 100), loadout.processorPriorities())
    }

    private val weaponModule =
        object : SimulationToolModule {
            override fun claims(stack: ItemStack) = stack.`is`(Items.DIAMOND_SWORD) || stack.`is`(Items.NETHERITE_SWORD)

            override fun contribute(
                stack: ItemStack,
                builder: SimulationToolLoadout.Builder,
            ) = builder.weapon(stack)
        }

    private fun rejectModule(
        trigger: net.minecraft.world.item.Item,
        rejected: net.minecraft.world.item.Item,
    ): SimulationToolModule =
        object : SimulationToolModule {
            override fun claims(stack: ItemStack) = stack.`is`(trigger)

            override fun contribute(
                stack: ItemStack,
                builder: SimulationToolLoadout.Builder,
            ) = builder.processOutputs { _, output ->
                if (output.`is`(rejected)) emptyList() else listOf(output)
            }
        }

    private fun processorModule(
        trigger: net.minecraft.world.item.Item,
        priority: Int,
    ): SimulationToolModule =
        object : SimulationToolModule {
            override fun claims(stack: ItemStack): Boolean = stack.`is`(trigger)

            override fun contribute(
                stack: ItemStack,
                builder: SimulationToolLoadout.Builder,
            ) = builder.processOutputs(priority) { _, output -> listOf(output) }
        }
}
