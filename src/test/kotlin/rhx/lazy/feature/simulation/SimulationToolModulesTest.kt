package rhx.lazy.feature.simulation

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
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
    fun `output filters from different tools combine`() {
        val loadout =
            SimulationToolModules.buildLoadout(
                listOf(rejectModule(Items.DIAMOND_SWORD, Items.BONE), rejectModule(Items.LAVA_BUCKET, Items.ROTTEN_FLESH)),
                listOf(ItemStack(Items.DIAMOND_SWORD), ItemStack(Items.LAVA_BUCKET)),
            )

        assertFalse(loadout.acceptsOutput(ItemStack(Items.BONE)))
        assertFalse(loadout.acceptsOutput(ItemStack(Items.ROTTEN_FLESH)))
        assertTrue(loadout.acceptsOutput(ItemStack(Items.STRING)))
    }

    @Test
    fun `batch settlement upgrade combines with other tools`() {
        val loadout =
            SimulationToolModules.buildLoadout(
                listOf(settleBatchModule, weaponModule),
                listOf(ItemStack(Items.DISPENSER), ItemStack(Items.DIAMOND_SWORD)),
            )

        assertTrue(loadout.settlesBatchImmediately)
        assertTrue(loadout.weapon.`is`(Items.DIAMOND_SWORD))
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
        assertFalse(loadout.settlesBatchImmediately)
        assertTrue(loadout.weapon.isEmpty)
        assertTrue(loadout.acceptsOutput(ItemStack(Items.DIAMOND_SWORD)))
    }

    @Test
    fun `an empty stack is never claimed`() {
        assertFalse(SimulationToolModules.claims(ItemStack.EMPTY))
    }

    private val weaponModule =
        object : SimulationToolModule {
            override fun claims(stack: ItemStack) = stack.`is`(Items.DIAMOND_SWORD) || stack.`is`(Items.NETHERITE_SWORD)

            override fun contribute(
                stack: ItemStack,
                builder: SimulationToolLoadout.Builder,
            ) = builder.weapon(stack)
        }

    private val settleBatchModule =
        object : SimulationToolModule {
            override fun claims(stack: ItemStack) = stack.`is`(Items.DISPENSER)

            override fun contribute(
                stack: ItemStack,
                builder: SimulationToolLoadout.Builder,
            ) = builder.settleBatchImmediately()
        }

    private fun rejectModule(
        trigger: net.minecraft.world.item.Item,
        rejected: net.minecraft.world.item.Item,
    ) = object : SimulationToolModule {
        override fun claims(stack: ItemStack) = stack.`is`(trigger)

        override fun contribute(
            stack: ItemStack,
            builder: SimulationToolLoadout.Builder,
        ) = builder.rejectOutputs { output -> output.`is`(rejected) }
    }
}
