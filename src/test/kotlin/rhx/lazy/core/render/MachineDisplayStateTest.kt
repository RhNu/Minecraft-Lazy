package rhx.lazy.core.render

import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MachineDisplayStateTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `round trips icon and activity`() {
        val state = MachineDisplayState(ItemStack(Items.WHEAT_SEEDS), MachineActivity.BLOCKED)

        val restored = MachineDisplayState.parse(registries, state.save(registries))

        assertTrue(restored.matches(state))
        assertEquals(Items.WHEAT_SEEDS, restored.icon.item)
        assertEquals(MachineActivity.BLOCKED, restored.activity)
    }

    @Test
    fun `round trips an empty icon so a cleared target reaches the client`() {
        val restored = MachineDisplayState.parse(registries, MachineDisplayState.EMPTY.save(registries))

        assertTrue(restored.isEmpty)
        assertEquals(MachineActivity.IDLE, restored.activity)
    }

    @Test
    fun `matches compares stack contents rather than identity`() {
        val running = MachineDisplayState(ItemStack(Items.WHEAT_SEEDS), MachineActivity.RUNNING)

        assertTrue(running.matches(MachineDisplayState(ItemStack(Items.WHEAT_SEEDS), MachineActivity.RUNNING)))
        assertFalse(running.matches(MachineDisplayState(ItemStack(Items.WHEAT_SEEDS), MachineActivity.IDLE)))
        assertFalse(running.matches(MachineDisplayState(ItemStack(Items.BEETROOT_SEEDS), MachineActivity.RUNNING)))
    }

    @Test
    fun `a tag from an unknown sender decodes as an idle blank`() {
        val restored = MachineDisplayState.parse(registries, CompoundTag().apply { putByte("activity", 9) })

        assertTrue(restored.isEmpty)
        assertEquals(MachineActivity.IDLE, restored.activity)
    }
}
