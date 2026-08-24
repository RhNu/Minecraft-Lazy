package rhx.lazy.feature.shaping

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShaperBlockEntityPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `dropped machine keeps lanes but resets its phantom sample`() {
        val source = newShaper()
        mutableField<ItemStack>(source, "inputTemplates")[0] = ItemStack(Items.IRON_INGOT)
        mutableField<Int>(source, "inputCounts")[0] = 700
        setField(source, "sample", ItemStack(Items.IRON_NUGGET))

        assertTrue(source.hasStoredContents())
        assertTrue(source.hasSample())

        val dropped = ItemStack(ShaperRegistries.item.get())
        source.saveContentsToItem(dropped, registries)
        val data = requireNotNull(dropped.get(DataComponents.BLOCK_ENTITY_DATA))
        assertFalse(data.copyTag().contains("sample"))

        val restored = newShaper()
        restored.loadWithComponents(data.copyTag(), registries)
        assertEquals(700, restored.inputHandler.getStackInSlot(0).count)
        assertFalse(restored.hasSample())
    }

    @Test
    fun `sample alone is a setting and does not make the drop stateful`() {
        val source = newShaper()
        setField(source, "sample", ItemStack(Items.IRON_NUGGET))

        assertFalse(source.hasStoredContents())
    }

    private fun newShaper(): ShaperBlockEntity =
        ShaperBlockEntity(
            BlockPos.ZERO,
            ShaperRegistries.block.get().defaultBlockState(),
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> mutableField(
        owner: Any,
        name: String,
    ): MutableList<T> {
        val field = owner.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(owner) as MutableList<T>
    }

    private fun setField(
        owner: Any,
        name: String,
        value: Any,
    ) {
        val field = owner.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(owner, value)
    }
}
