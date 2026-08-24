package rhx.lazy.feature.repairer

import net.minecraft.world.item.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ItemRepairHookRegistryTest {
    @Test
    fun `hooks run in order and report failures without stopping later hooks`() {
        val calls = mutableListOf<String>()
        val registry = ItemRepairHookRegistry()
        registry.register { _, _ ->
            calls += "first"
            throw IllegalStateException("integration failure")
        }
        registry.register { _, _ ->
            calls += "last"
            ItemRepairHookResult.Success
        }

        val result = registry.afterRepair(ItemStack.EMPTY, null)

        assertSame(ItemRepairHookResult.Failed, result)
        assertEquals(listOf("first", "last"), calls)
    }

    @Test
    fun `same hook instance cannot be registered twice`() {
        val registry = ItemRepairHookRegistry()
        val hook = ItemRepairHook { _, _ -> ItemRepairHookResult.Success }
        registry.register(hook)

        val failure =
            try {
                registry.register(hook)
                null
            } catch (exception: IllegalStateException) {
                exception
            }

        assertNotNull(failure)
    }
}
