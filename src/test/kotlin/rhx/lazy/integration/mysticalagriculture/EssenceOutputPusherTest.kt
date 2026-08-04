package rhx.lazy.integration.mysticalagriculture

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.ItemStackHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EssenceOutputPusherTest {
    @Test
    fun `blocked whole stack is not inserted partially`() {
        val handler = ItemStackHandler(1)
        handler.setStackInSlot(0, ItemStack(Items.STONE, 63))

        assertFalse(EssenceOutputPusher.pushWholeStack(handler, ItemStack(Items.STONE, 2)))
        assertEquals(63, handler.getStackInSlot(0).count)
    }

    @Test
    fun `fully accepted whole stack is inserted`() {
        val handler = ItemStackHandler(1)

        assertTrue(EssenceOutputPusher.pushWholeStack(handler, ItemStack(Items.STONE, 2)))
        assertEquals(2, handler.getStackInSlot(0).count)
    }
}
