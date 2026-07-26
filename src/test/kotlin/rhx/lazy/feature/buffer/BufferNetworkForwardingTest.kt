package rhx.lazy.feature.buffer

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import rhx.lazy.integration.beyonddimensions.FakeDimensionNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BufferNetworkForwardingTest {
    @Test
    fun `enabling forwarding moves existing contents and keeps network remainders`() {
        val storage =
            FakeDimensionNetworkStorage().apply {
                itemCapacity = 100
                fluidCapacity = 20_000
            }
        val buffer = newBuffer(storage)
        buffer.itemHandler.insertItem(0, ItemStack(Items.DIAMOND, 180), false)
        buffer.fluidHandler.fill(
            FluidStack(Fluids.WATER, 40_000),
            IFluidHandler.FluidAction.EXECUTE,
        )

        buffer.enableNetworkForwarding(FakeDimensionNetworkStorage.TEST_NETWORK_ID)

        assertEquals(100, storage.storedItemAmount)
        assertEquals(80, buffer.getItemCount(0))
        assertEquals(20_000, storage.storedFluidAmount)
        assertEquals(20_000, buffer.getFluid(0).amount)
        assertTrue(buffer.isNetworkForwardingEnabled)
    }

    @Test
    fun `new inputs go to the network before using local capacity`() {
        val storage =
            FakeDimensionNetworkStorage().apply {
                itemCapacity = 100
                fluidCapacity = 20_000
            }
        val buffer = newBuffer(storage)
        buffer.enableNetworkForwarding(FakeDimensionNetworkStorage.TEST_NETWORK_ID)

        val itemRemainder = buffer.itemHandler.insertItem(0, ItemStack(Items.EMERALD, 180), false)
        val fluidAccepted =
            buffer.fluidHandler.fill(
                FluidStack(Fluids.LAVA, 40_000),
                IFluidHandler.FluidAction.EXECUTE,
            )

        assertTrue(itemRemainder.isEmpty)
        assertEquals(100, storage.storedItemAmount)
        assertEquals(80, buffer.getItemCount(0))
        assertEquals(40_000, fluidAccepted)
        assertEquals(20_000, storage.storedFluidAmount)
        assertEquals(20_000, buffer.getFluid(0).amount)

        storage.itemCapacity = 120
        val differentItemRemainder =
            buffer.itemHandler.insertItem(
                0,
                ItemStack(Items.DIAMOND, 20),
                false,
            )
        assertTrue(differentItemRemainder.isEmpty)
        assertEquals(120, storage.storedItemAmount)
        assertEquals(80, buffer.getItemCount(0))
    }

    @Test
    fun `network remainder never enters an incompatible local slot`() {
        val storage =
            FakeDimensionNetworkStorage().apply {
                itemCapacity = 0
            }
        val buffer = newBuffer(storage)
        buffer.itemHandler.insertItem(0, ItemStack(Items.DIAMOND, 80), false)
        buffer.enableNetworkForwarding(FakeDimensionNetworkStorage.TEST_NETWORK_ID)
        storage.itemCapacity = 10

        val simulatedRemainder =
            buffer.itemHandler.insertItem(
                0,
                ItemStack(Items.EMERALD, 20),
                true,
            )
        assertEquals(10, simulatedRemainder.count)
        assertTrue(ItemStack.isSameItemSameComponents(ItemStack(Items.EMERALD), simulatedRemainder))
        assertEquals(0, storage.storedItemAmount)
        assertEquals(80, buffer.getItemCount(0))

        val remainder =
            buffer.itemHandler.insertItem(
                0,
                ItemStack(Items.EMERALD, 20),
                false,
            )
        assertEquals(10, remainder.count)
        assertTrue(ItemStack.isSameItemSameComponents(ItemStack(Items.EMERALD), remainder))
        assertEquals(10, storage.storedItemAmount)
        assertEquals(80, buffer.getItemCount(0))
        assertTrue(
            ItemStack.isSameItemSameComponents(
                ItemStack(Items.DIAMOND),
                buffer.getItemTemplate(0),
            ),
        )
    }

    @Test
    fun `simulation mutates neither network nor buffer`() {
        val storage = FakeDimensionNetworkStorage()
        val buffer = newBuffer(storage)
        buffer.enableNetworkForwarding(FakeDimensionNetworkStorage.TEST_NETWORK_ID)

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
        assertTrue(buffer.isNetworkForwardingEnabled)
    }

    @Test
    fun `missing network disables forwarding and falls back to local storage`() {
        val storage = FakeDimensionNetworkStorage()
        val buffer = newBuffer(storage)
        buffer.enableNetworkForwarding(FakeDimensionNetworkStorage.TEST_NETWORK_ID)
        storage.networkExists = false

        val remainder = buffer.itemHandler.insertItem(0, ItemStack(Items.IRON_INGOT, 24), false)

        assertTrue(remainder.isEmpty)
        assertEquals(24, buffer.getItemCount(0))
        assertFalse(buffer.isNetworkForwardingEnabled)
        assertEquals(null, buffer.boundDimensionNetworkId)
    }

    private fun newBuffer(storage: FakeDimensionNetworkStorage): BufferBlockEntity =
        BufferBlockEntity(
            BlockPos.ZERO,
            BufferRegistries.block.get().defaultBlockState(),
            storage,
        )
}
