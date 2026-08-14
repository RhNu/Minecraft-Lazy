package rhx.lazy.feature.buffer

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import rhx.lazy.core.io.IoMode
import rhx.lazy.core.testing.FakeNetworkOutputProvider
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BufferNetworkForwardingTest {
    @Test
    fun `network route drains existing items and fluids and keeps remainders`() {
        val storage =
            FakeNetworkStorage().apply {
                itemCapacity = 100
                fluidCapacity = 20_000
            }
        val buffer = newBuffer()
        buffer.itemHandler.insertItem(0, ItemStack(Items.DIAMOND, 180), false)
        buffer.fluidHandler.fill(
            FluidStack(Fluids.WATER, 40_000),
            IFluidHandler.FluidAction.EXECUTE,
        )
        setNetworkRoute(buffer, storage)

        buffer.onServerTick()

        assertEquals(100, storage.storedItemAmount)
        assertEquals(80, buffer.getItemCount(0))
        assertEquals(20_000, storage.storedFluidAmount)
        assertEquals(20_000, buffer.getFluid(0).amount)
        assertEquals(IoMode.NETWORK, buffer.ioController.mode)
    }

    @Test
    fun `network route does not intercept new input before the outbound tick`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = 100 }
        val buffer = newBuffer()
        setNetworkRoute(buffer, storage)

        val remainder = buffer.itemHandler.insertItem(0, ItemStack(Items.EMERALD, 180), false)

        assertTrue(remainder.isEmpty)
        assertEquals(180, buffer.getItemCount(0))
        assertEquals(0, storage.storedItemAmount)

        buffer.onServerTick()

        assertEquals(100, storage.storedItemAmount)
        assertEquals(80, buffer.getItemCount(0))
    }

    @Test
    fun `network remainder stays in the local buffer`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = 10 }
        val buffer = newBuffer()
        buffer.itemHandler.insertItem(0, ItemStack(Items.DIAMOND, 80), false)
        setNetworkRoute(buffer, storage)

        buffer.onServerTick()

        assertEquals(10, storage.storedItemAmount)
        assertEquals(70, buffer.getItemCount(0))
        assertEquals(IoMode.NETWORK, buffer.ioController.mode)
    }

    @Test
    fun `local simulation mutates neither network nor buffer`() {
        val storage = FakeNetworkStorage()
        val buffer = newBuffer()
        setNetworkRoute(buffer, storage)

        val itemRemainder = buffer.itemHandler.insertItem(0, ItemStack(Items.GOLD_INGOT, 32), true)
        val fluidAccepted =
            buffer.fluidHandler.fill(
                FluidStack(Fluids.WATER, 8_000),
                IFluidHandler.FluidAction.SIMULATE,
            )

        assertTrue(itemRemainder.isEmpty)
        assertEquals(8_000, fluidAccepted)
        assertEquals(0, storage.storedItemAmount)
        assertEquals(0, storage.storedFluidAmount)
        assertEquals(0, buffer.totalItemCount)
        assertEquals(0, buffer.totalFluidAmount)
        assertEquals(IoMode.NETWORK, buffer.ioController.mode)
    }

    @Test
    fun `missing network falls back to passive without changing local contents`() {
        val storage = FakeNetworkStorage()
        val buffer = newBuffer()
        buffer.itemHandler.insertItem(0, ItemStack(Items.IRON_INGOT, 24), false)
        setNetworkRoute(buffer, storage)
        storage.networkExists = false

        buffer.onServerTick()

        assertEquals(24, buffer.getItemCount(0))
        assertEquals(IoMode.PASSIVE, buffer.ioController.mode)
        assertFalse(buffer.ioController.networkPaused)
    }

    private fun setNetworkRoute(
        buffer: BufferBlockEntity,
        storage: FakeNetworkStorage,
    ) {
        assertTrue(buffer.ioController.setNetworkTarget(FakeNetworkOutputProvider(storage).target))
    }

    private fun newBuffer(): BufferBlockEntity =
        BufferBlockEntity(
            BlockPos.ZERO,
            BufferRegistries.block.get().defaultBlockState(),
        )
}
