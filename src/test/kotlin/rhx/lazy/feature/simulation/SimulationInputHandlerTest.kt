package rhx.lazy.feature.simulation

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationInputHandlerTest {
    @Test
    fun `target slot accepts one item and simulation does not mutate it`() {
        val stacks = MutableList(2) { ItemStack.EMPTY }
        var changes = 0
        val handler =
            SimulationInputHandler(
                stacks,
                { slot, _ -> if (slot == 0) 1 else 64 },
                { slot, stack -> slot == 0 && stack.`is`(Items.WHEAT) || slot == 1 && stack.`is`(Items.DIAMOND) },
                { slot, stack ->
                    stacks[slot] = stack
                    changes++
                },
            )

        val simulatedRemainder = handler.insertItem(0, ItemStack(Items.WHEAT, 4), true)
        assertEquals(3, simulatedRemainder.count)
        assertTrue(stacks[0].isEmpty)

        val remainder = handler.insertItem(0, ItemStack(Items.WHEAT, 4), false)
        assertEquals(3, remainder.count)
        assertEquals(1, stacks[0].count)
        assertEquals(1, changes)
    }

    @Test
    fun `invalid direct assignment is ignored`() {
        val stacks = MutableList(1) { ItemStack.EMPTY }
        val handler =
            SimulationInputHandler(
                stacks,
                { _, _ -> 1 },
                { _, stack -> stack.`is`(Items.WHEAT) },
                { slot, stack -> stacks[slot] = stack },
            )

        handler.setStackInSlot(0, ItemStack(Items.DIAMOND))

        assertTrue(stacks[0].isEmpty)
    }
}
