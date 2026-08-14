package rhx.lazy.feature.buffer

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
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

class BufferBlockEntityPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `buffer managed data preserves components and extended capacities`() {
        val source = newBuffer()
        val item = ItemStack(Items.DIAMOND, 200)
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Managed diamond"))
        source.itemHandler.insertItem(0, item, false)

        val fluid = FluidStack(Fluids.WATER, 48_000)
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Managed water"))
        source.fluidHandler.fill(fluid, IFluidHandler.FluidAction.EXECUTE)

        val restored = newBuffer()
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        assertEquals(200, restored.getItemCount(0))
        assertEquals("Managed diamond", restored.getItemTemplate(0).hoverName.string)
        assertEquals(48_000, restored.getFluid(0).amount)
        assertEquals(
            "Managed water",
            restored.getFluid(0).get(DataComponents.CUSTOM_NAME)?.string,
        )
        assertEquals(200, restored.totalItemCount)
        assertEquals(48_000, restored.totalFluidAmount)
    }

    @Test
    fun `buffer normalizes malformed managed collection lengths and capacities`() {
        val source = newBuffer()
        mutableField<ItemStack>(source, "itemTemplates").apply {
            clear()
            add(ItemStack(Items.STONE, 2))
        }
        mutableField<Int>(source, "itemCounts").apply {
            clear()
            add(999)
            add(-20)
            repeat(10) { add(42) }
        }
        mutableField<FluidStack>(source, "fluids").apply {
            clear()
            add(FluidStack(Fluids.WATER, 100_000))
        }

        val restored = newBuffer()
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        assertEquals(BufferBlockEntity.ITEM_SLOT_CAPACITY, restored.getItemCount(0))
        assertEquals(0, restored.getItemCount(1))
        assertEquals(BufferBlockEntity.FLUID_TANK_CAPACITY, restored.getFluid(0).amount)
        repeat(BufferBlockEntity.ITEM_SLOT_COUNT) { restored.getItemCount(it) }
        repeat(BufferBlockEntity.FLUID_TANK_COUNT) { restored.getFluid(it) }
        assertEquals(BufferBlockEntity.ITEM_SLOT_CAPACITY, restored.totalItemCount)
        assertEquals(BufferBlockEntity.FLUID_TANK_CAPACITY, restored.totalFluidAmount)

        assertTrue(restored.clearContents())
        assertFalse(restored.hasContents())
        assertFalse(restored.clearContents())
    }

    @Test
    fun `buffer block item carries managed data for placement`() {
        val source = newBuffer()
        source.itemHandler.insertItem(0, ItemStack(Items.EMERALD, 180), false)
        source.fluidHandler.fill(
            FluidStack(Fluids.LAVA, 32_000),
            IFluidHandler.FluidAction.EXECUTE,
        )
        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())
        source.ioController.setNetworkTarget(provider.target)

        val dropped = ItemStack(BufferRegistries.item.get())
        source.saveToItem(dropped, registries)
        val blockEntityData = requireNotNull(dropped.get(DataComponents.BLOCK_ENTITY_DATA))

        val restored = newBuffer()
        restored.loadWithComponents(blockEntityData.copyTag(), registries)

        assertEquals(180, restored.getItemCount(0))
        assertEquals(32_000, restored.getFluid(0).amount)
        assertEquals(IoMode.NETWORK, restored.ioController.mode)
        assertEquals(
            FakeNetworkStorage.TEST_NETWORK_ID.value,
            restored.ioController.target
                ?.data
                ?.getInt("networkId"),
        )
    }

    private fun newBuffer(): BufferBlockEntity =
        BufferBlockEntity(
            BlockPos.ZERO,
            BufferRegistries.block.get().defaultBlockState(),
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
}
