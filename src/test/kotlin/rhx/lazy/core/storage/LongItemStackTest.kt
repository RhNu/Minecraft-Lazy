package rhx.lazy.core.storage

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LongItemStackTest {
    @Test
    fun `template is normalized and copied`() {
        val source = ItemStack(Items.DIAMOND, 32)
        val stored = LongItemStack(source, 100L)

        source.count = 1
        val template = stored.template
        template.count = 10

        assertEquals(1, stored.template.count)
        assertTrue(stored.matches(ItemStack(Items.DIAMOND)))
    }

    @Test
    fun `addition saturates instead of overflowing`() {
        val stored = LongItemStack(ItemStack(Items.DIAMOND), Long.MAX_VALUE - 4L)

        assertEquals(Long.MAX_VALUE, stored.plus(10L).count)
    }

    @Test
    fun `materialization creates legal item stacks`() {
        val stacks = LongItemStack(ItemStack(Items.DIAMOND), 130L).toItemStacks().toList()

        assertEquals(listOf(64, 64, 2), stacks.map(ItemStack::getCount))
    }
}
