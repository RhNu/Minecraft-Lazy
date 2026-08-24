package rhx.lazy.core.configurator

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.UseOnContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ModularConfiguratorModuleRegistryTest {
    @Test
    fun `modules retain registration order and duplicate ids fail`() {
        val registry = ModularConfiguratorModuleRegistry()
        val first = module("first") { stack -> stack.`is`(Items.DIAMOND) }
        val second = module("second") { stack -> stack.`is`(Items.EMERALD) }

        registry.register(first)
        registry.register(second)

        assertEquals(listOf(first.id, second.id), registry.snapshot().map(ModularConfiguratorModule::id))
        try {
            registry.register(module("first") { true })
            fail("Expected duplicate module id to fail")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun `accepted materials are the union of loaded modules`() {
        val registry = ModularConfiguratorModuleRegistry()
        registry.register(module("diamond") { stack -> stack.`is`(Items.DIAMOND) })
        registry.register(module("emerald") { stack -> stack.`is`(Items.EMERALD) })

        assertTrue(registry.acceptsMaterial(ItemStack(Items.DIAMOND)))
        assertTrue(registry.acceptsMaterial(ItemStack(Items.EMERALD)))
        assertFalse(registry.acceptsMaterial(ItemStack(Items.REDSTONE)))
    }

    private fun module(
        path: String,
        accepts: (ItemStack) -> Boolean,
    ) = object : ModularConfiguratorModule {
        override val id = ResourceLocation.fromNamespaceAndPath("lazy_test", path)

        override fun acceptsMaterial(stack: ItemStack): Boolean = accepts(stack)

        override fun useOn(context: UseOnContext): InteractionResult? = null
    }
}
