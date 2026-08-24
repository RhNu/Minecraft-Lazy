package rhx.lazy.core.resource

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceStoreTest {
    @Test
    fun `store merges matching variants before occupying an empty entry`() {
        val store = ResourceStore(ItemResourceKind, slots = 2, amountLimit = 10)
        val diamonds = requireNotNull(itemAmount(ItemStack(Items.DIAMOND), 7))

        assertEquals(7, store.insert(diamonds))
        assertEquals(8, store.insert(diamonds.withAmount(8)))
        assertEquals(10, store.amount(0))
        assertEquals(5, store.amount(1))
    }

    @Test
    fun `failed delta leaves the store unchanged`() {
        val store = ResourceStore(ItemResourceKind, slots = 1, amountLimit = 10)
        val diamonds = requireNotNull(itemAmount(ItemStack(Items.DIAMOND), 5))
        val emeralds = requireNotNull(itemAmount(ItemStack(Items.EMERALD), 1))
        store.insert(diamonds)

        assertFalse(
            store.tryApply(
                ResourceDelta(
                    extracted = listOf(diamonds.withAmount(3)),
                    inserted = listOf(emeralds),
                ),
            ),
        )
        assertEquals(5, store.amount(0))
        assertTrue(store.variant(0)?.matches(requireNotNull(ItemVariant.of(ItemStack(Items.DIAMOND)))) == true)
    }

    @Test
    fun `multi store transaction is atomic`() {
        val input = ResourceStore(ItemResourceKind, slots = 1, amountLimit = 10)
        val output = ResourceStore(ItemResourceKind, slots = 1, amountLimit = 1)
        val iron = requireNotNull(itemAmount(ItemStack(Items.IRON_INGOT), 2))
        val plates = requireNotNull(itemAmount(ItemStack(Items.IRON_NUGGET), 2))
        input.insert(iron)

        assertFalse(
            ResourceTransaction.tryApply(
                StoreDelta(input, ResourceDelta(extracted = listOf(iron))),
                StoreDelta(output, ResourceDelta(inserted = listOf(plates))),
            ),
        )
        assertEquals(2, input.amount(0))
        assertEquals(0, output.amount(0))
    }

    @Test
    fun `item variants include data components in identity`() {
        val named =
            ItemStack(Items.DIAMOND).apply {
                set(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData
                        .of(
                            CompoundTag().apply {
                                putInt("x", 1)
                            },
                        ),
                )
            }
        val plain = requireNotNull(ItemVariant.of(ItemStack(Items.DIAMOND)))
        val custom = requireNotNull(ItemVariant.of(named))

        assertFalse(plain.matches(custom))
    }
}
