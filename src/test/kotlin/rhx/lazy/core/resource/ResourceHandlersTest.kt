package rhx.lazy.core.resource

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceHandlersTest {
    @Test
    fun `item capability inserts into the requested slot`() {
        val store = ResourceStore(ItemResourceKind, 2, 100)
        val handler = ResourceItemHandler(store)

        handler.insertItem(1, ItemStack(Items.STONE, 10), false)
        handler.insertItem(1, ItemStack(Items.STONE, 15), false)

        assertEquals(0L, store.amount(0))
        assertEquals(25L, store.amount(1))
    }

    @Test
    fun `item capability exposes and extracts only a legal vanilla stack`() {
        val store = ResourceStore(ItemResourceKind, 1)
        store.insert(requireNotNull(itemAmount(ItemStack(Items.STONE), Int.MAX_VALUE.toLong() + 20)))
        val handler = ResourceItemHandler(store, allowInsert = false)

        assertEquals(64, handler.getStackInSlot(0).count)
        assertEquals(64, handler.extractItem(0, Int.MAX_VALUE, false).count)
        assertEquals(Int.MAX_VALUE.toLong() + 20 - 64, store.amount(0))
    }

    @Test
    fun `modifiable item capability replaces and clears a slot directly`() {
        val store = ResourceStore(ItemResourceKind, 1, 100)
        store.insert(requireNotNull(itemAmount(ItemStack(Items.STONE), 80)))
        val handler = ResourceItemHandler(store, allowInsert = false)

        handler.setStackInSlot(0, ItemStack(Items.DIAMOND, 3))
        assertEquals(3L, store.amount(0))
        assertEquals(Items.DIAMOND, store.variant(0)?.template?.item)

        handler.setStackInSlot(0, ItemStack.EMPTY)
        assertEquals(0L, store.amount(0))
    }

    @Test
    fun `fluid capability chunks long storage at the int boundary`() {
        val store = ResourceStore(FluidResourceKind, 1)
        store.insert(requireNotNull(fluidAmount(FluidStack(Fluids.WATER, 1), Int.MAX_VALUE.toLong() + 20)))
        val handler = ResourceFluidHandler(store, allowInsert = false)

        assertEquals(Int.MAX_VALUE, handler.getFluidInTank(0).amount)
        assertEquals(Int.MAX_VALUE, handler.drain(Int.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).amount)
        assertEquals(20L, store.amount(0))
    }

    @Test
    fun `fluid capability drains one request across matching tanks`() {
        val store = ResourceStore(FluidResourceKind, slots = 2, amountLimit = 10)
        store.insert(requireNotNull(fluidAmount(FluidStack(Fluids.WATER, 1), 16)))
        val handler = ResourceFluidHandler(store, allowInsert = false)

        assertEquals(16, handler.drain(FluidStack(Fluids.WATER, 16), IFluidHandler.FluidAction.SIMULATE).amount)
        assertEquals(16L, store.snapshot().sumOf { it.amount })
        assertEquals(16, handler.drain(16, IFluidHandler.FluidAction.EXECUTE).amount)
        assertEquals(0L, store.snapshot().sumOf { it.amount })
    }
}
