package rhx.lazy.feature.itemcopier

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.ItemStackHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItemCopierPusherTest {
    @Test
    fun `pushes one maximum stack to every available handler`() {
        val handlers = List(6) { ItemStackHandler(1) }

        ItemCopierPusher.pushToHandlers(ItemStack(Items.DIAMOND), handlers)

        handlers.forEach { handler ->
            assertEquals(Items.DIAMOND, handler.getStackInSlot(0).item)
            assertEquals(64, handler.getStackInSlot(0).count)
        }
    }

    @Test
    fun `accepts partial insertion and ignores missing or full handlers`() {
        val partial = ItemStackHandler(1)
        partial.setStackInSlot(0, ItemStack(Items.STONE, 60))
        val full = ItemStackHandler(1)
        full.setStackInSlot(0, ItemStack(Items.STONE, 64))

        ItemCopierPusher.pushToHandlers(
            ItemStack(Items.STONE),
            listOf(partial, null, full),
        )

        assertEquals(64, partial.getStackInSlot(0).count)
        assertEquals(64, full.getStackInSlot(0).count)
    }

    @Test
    fun `uses the template item maximum stack size`() {
        val handler = ItemStackHandler(1)

        ItemCopierPusher.pushToHandlers(ItemStack(Items.DIAMOND_SWORD), listOf(handler))

        assertEquals(1, handler.getStackInSlot(0).count)
    }

    @Test
    fun `empty template does not touch handlers`() {
        val handler =
            object : ItemStackHandler(1) {
                override fun insertItem(
                    slot: Int,
                    stack: ItemStack,
                    simulate: Boolean,
                ): ItemStack = error("empty template must not attempt insertion")
            }

        ItemCopierPusher.pushToHandlers(ItemStack.EMPTY, listOf(handler))

        assertTrue(handler.getStackInSlot(0).isEmpty)
    }
}
