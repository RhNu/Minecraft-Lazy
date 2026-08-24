package rhx.lazy.feature.shaping

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShaperLanesTest {
    @Test
    fun `pool merges matching items before occupying empty lanes`() {
        val templates = MutableList(3) { ItemStack.EMPTY }
        val counts = MutableList(3) { 0 }
        val lanes = ShaperLanes(templates, counts, 3, 10) {}

        assertEquals(8, lanes.insert(ItemStack(Items.IRON_INGOT), 8))
        assertEquals(7, lanes.insert(ItemStack(Items.IRON_INGOT), 7))

        assertEquals(10, lanes.count(0))
        assertEquals(5, lanes.count(1))
        assertTrue(lanes.template(2).isEmpty)
    }

    @Test
    fun `handler extracts only representable vanilla stacks from large lanes`() {
        val templates = mutableListOf(ItemStack.EMPTY)
        val counts = mutableListOf(0)
        val lanes = ShaperLanes(templates, counts, 1, 1024) {}
        lanes.insert(ItemStack(Items.DIAMOND), 1000)
        val handler = ShaperLaneHandler(lanes, allowInsert = false) { false }

        assertEquals(64, handler.extractItem(0, 1000, false).count)
        assertEquals(936, lanes.count(0))
    }
}
