package rhx.lazy.core.configurator

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModularConfiguratorInventoryTest {
    @Test
    fun `matching materials accumulate to 1024 and simulation does not mutate`() {
        val configurator = ItemStack(ModularConfiguratorRegistries.item.get())
        val inventory = ModularConfiguratorInventory(configurator) { stack -> stack.`is`(Items.REDSTONE) }

        repeat(15) { assertTrue(inventory.insertItem(0, ItemStack(Items.REDSTONE, 64), false).isEmpty) }
        assertTrue(inventory.insertItem(0, ItemStack(Items.REDSTONE, 64), true).isEmpty)
        assertEquals(960, inventory.getStackInSlot(0).count)
        assertTrue(inventory.insertItem(0, ItemStack(Items.REDSTONE, 64), false).isEmpty)

        assertEquals(1024, inventory.getStackInSlot(0).count)
        assertEquals(64, inventory.extractItem(0, 64, false).count)
        assertEquals(960, inventory.getStackInSlot(0).count)
    }

    @Test
    fun `overflow incompatible and unclaimed materials are rejected`() {
        val configurator = ItemStack(ModularConfiguratorRegistries.item.get())
        val inventory = ModularConfiguratorInventory(configurator) { stack -> stack.`is`(Items.DIAMOND) }

        assertEquals(64, inventory.insertItem(0, ItemStack(Items.REDSTONE, 64), false).count)
        assertTrue(inventory.insertItem(0, ItemStack(Items.DIAMOND, 64), false).isEmpty)
        assertEquals(64, inventory.insertItem(0, ItemStack(Items.EMERALD, 64), false).count)
        inventory.setStackInSlot(0, ItemStack(Items.DIAMOND, 2000))

        assertEquals(1024, inventory.getStackInSlot(0).count)
        assertEquals(1, inventory.extractItem(0, 1, true).count)
        assertEquals(1024, inventory.getStackInSlot(0).count)
    }

    @Test
    fun `empty module set accepts no materials`() {
        val configurator = ItemStack(ModularConfiguratorRegistries.item.get())
        val inventory = ModularConfiguratorInventory(configurator) { false }

        assertEquals(64, inventory.insertItem(0, ItemStack(Items.DIAMOND, 64), false).count)
        assertTrue(inventory.getStackInSlot(0).isEmpty)
    }
}
