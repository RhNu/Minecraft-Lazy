package rhx.lazy.feature.itemcopier

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.ManagedBlockEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemCopierBlockEntityTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `template keeps components without consuming or retaining source count`() {
        val copier = newCopier()
        val source = ItemStack(Items.DIAMOND, 37)
        source.set(DataComponents.CUSTOM_NAME, Component.literal("Original diamond"))

        assertFalse(copier.hasTemplate())
        copier.setTemplate(source)

        assertEquals(37, source.count)
        assertEquals(1, copier.getTemplate().count)
        assertEquals("Original diamond", copier.getTemplate().hoverName.string)
        assertTrue(copier.hasTemplate())

        copier.clearTemplate()

        assertTrue(copier.getTemplate().isEmpty)
        assertFalse(copier.hasTemplate())
    }

    @Test
    fun `empty copier is treated as blank even when gear changed`() {
        val copier = newCopier()

        copier.setGear(ItemCopierGear.VERY_SLOW)

        assertFalse(copier.hasTemplate())
    }

    @Test
    fun `gear cycles through the confirmed intervals`() {
        val copier = newCopier()

        assertEquals(20, copier.getGear().intervalTicks)
        copier.cycleGear()
        assertEquals(100, copier.getGear().intervalTicks)
        copier.cycleGear()
        assertEquals(200, copier.getGear().intervalTicks)
        copier.cycleGear()
        assertEquals(10, copier.getGear().intervalTicks)
        copier.cycleGear()
        assertEquals(20, copier.getGear().intervalTicks)
    }

    @Test
    fun `schedule pushes immediately and then waits for the selected interval`() {
        val copier = newCopier()

        assertTrue(copier.advanceSchedule())
        repeat(ItemCopierGear.DEFAULT.intervalTicks - 1) {
            kotlin.test.assertFalse(copier.advanceSchedule())
        }
        assertTrue(copier.advanceSchedule())

        copier.setGear(ItemCopierGear.FAST)
        assertTrue(copier.advanceSchedule())
        repeat(ItemCopierGear.FAST.intervalTicks - 1) {
            kotlin.test.assertFalse(copier.advanceSchedule())
        }
        assertTrue(copier.advanceSchedule())
    }

    @Test
    fun `managed data and block item round trips preserve template and gear`() {
        val source = newCopier()
        val template = ItemStack(Items.DIAMOND_PICKAXE)
        template.set(DataComponents.CUSTOM_NAME, Component.literal("Copied pickaxe"))
        source.setTemplate(template)
        source.setGear(ItemCopierGear.VERY_SLOW)

        val dropped = ItemStack(ItemCopierRegistries.item.get())
        source.saveToItem(dropped, registries)
        val blockEntityData = requireNotNull(dropped.get(DataComponents.BLOCK_ENTITY_DATA))
        val managed =
            blockEntityData
                .copyTag()
                .getCompound(ManagedBlockEntity.MANAGED_DATA_KEY)
        val serializedTemplate =
            ItemStack.parseOptional(
                registries,
                managed.getCompound(ItemCopierBlockEntity.TEMPLATE_FIELD),
            )

        val restored = newCopier()
        restored.loadWithComponents(blockEntityData.copyTag(), registries)

        assertEquals("Copied pickaxe", serializedTemplate.hoverName.string)
        assertEquals(
            ItemCopierGear.VERY_SLOW.intervalTicks,
            managed.getInt(ItemCopierBlockEntity.PUSH_INTERVAL_FIELD),
        )
        assertEquals("Copied pickaxe", restored.getTemplate().hoverName.string)
        assertEquals(1, restored.getTemplate().count)
        assertEquals(ItemCopierGear.VERY_SLOW, restored.getGear())
        assertTrue(restored.advanceSchedule())
    }

    @Test
    fun `invalid persisted interval falls back to default`() {
        val source = newCopier()
        setPushInterval(source, 999)

        val restored = newCopier()
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        assertEquals(ItemCopierGear.DEFAULT, restored.getGear())
    }

    private fun newCopier(): ItemCopierBlockEntity =
        ItemCopierBlockEntity(
            BlockPos.ZERO,
            ItemCopierRegistries.block.get().defaultBlockState(),
        )

    private fun setPushInterval(
        copier: ItemCopierBlockEntity,
        interval: Int,
    ) {
        val field = ItemCopierBlockEntity::class.java.getDeclaredField("pushIntervalTicks")
        field.isAccessible = true
        field.setInt(copier, interval)
    }
}
