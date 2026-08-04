package rhx.lazy.integration.mysticalagriculture

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.ItemStackHandler
import kotlin.test.Test
import kotlin.test.assertEquals

class EssenceOutputPusherTest {
    @Test
    fun `partially accepted stack inserts the maximum capacity`() {
        val handler = ItemStackHandler(1)
        handler.setStackInSlot(0, ItemStack(Items.STONE, 63))

        assertEquals(1, EssenceOutputPusher.pushMaximum(handler, ItemStack(Items.STONE, 2)))
        assertEquals(64, handler.getStackInSlot(0).count)
    }

    @Test
    fun `fully accepted whole stack is inserted`() {
        val handler = ItemStackHandler(1)

        assertEquals(2, EssenceOutputPusher.pushMaximum(handler, ItemStack(Items.STONE, 2)))
        assertEquals(2, handler.getStackInSlot(0).count)
    }
}
