package rhx.lazy.feature.shaping

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.core.resource.itemAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShaperBlockEntityPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `dropped machine keeps lanes but resets its phantom sample`() {
        val source = newShaper()
        itemStore(source, "inputs").insert(requireNotNull(itemAmount(ItemStack(Items.IRON_INGOT), 700L)))
        setField(source, "sample", ItemStack(Items.IRON_NUGGET))

        assertTrue(source.hasStoredContents())
        assertTrue(source.hasSample())

        val dropped = ItemStack(ShaperRegistries.item.get())
        source.saveContentsToItem(dropped, registries)
        val data = requireNotNull(dropped.get(DataComponents.BLOCK_ENTITY_DATA))
        assertFalse(data.copyTag().contains("sample"))

        val restored = newShaper()
        restored.loadWithComponents(data.copyTag(), registries)
        assertEquals(700L, restored.inputAmount(0))
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

    private fun setField(
        owner: Any,
        name: String,
        value: Any,
    ) {
        val field = owner.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(owner, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun itemStore(
        owner: Any,
        name: String,
    ): ResourceStore<ItemVariant> {
        val field = owner.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(owner) as ResourceStore<ItemVariant>
    }
}
