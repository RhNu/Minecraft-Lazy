package rhx.lazy.feature.buffer

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import rhx.lazy.core.io.IoMode
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ItemVariant
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
    fun `buffer ignores malformed new store entries`() {
        val source = newBuffer()
        val saved = source.saveWithFullMetadata(registries)
        saved.put(
            "resourcesItems",
            ListTag().apply {
                add(storeEntry(0, ItemResourceKind.save(registries, requireNotNull(ItemVariant.of(ItemStack(Items.STONE)))), 999L))
                add(storeEntry(99, ItemResourceKind.save(registries, requireNotNull(ItemVariant.of(ItemStack(Items.DIAMOND)))), 1L))
            },
        )
        saved.put(
            "resourcesFluids",
            ListTag().apply {
                add(
                    storeEntry(
                        0,
                        FluidResourceKind.save(registries, requireNotNull(FluidVariant.of(FluidStack(Fluids.WATER, 1)))),
                        100_000L,
                    ),
                )
            },
        )

        val restored = newBuffer()
        restored.loadWithComponents(saved, registries)

        assertEquals(0, restored.getItemCount(0))
        assertEquals(0, restored.getItemCount(1))
        assertEquals(0, restored.getFluid(0).amount)
        repeat(BufferBlockEntity.ITEM_SLOT_COUNT) { restored.getItemCount(it) }
        repeat(BufferBlockEntity.FLUID_TANK_COUNT) { restored.getFluid(it) }
        assertEquals(0, restored.totalItemCount)
        assertEquals(0, restored.totalFluidAmount)

        assertFalse(restored.clearContents())
    }

    @Test
    fun `buffer block item carries contents but never its io settings`() {
        val source = newBuffer()
        source.itemHandler.insertItem(0, ItemStack(Items.EMERALD, 180), false)
        source.fluidHandler.fill(
            FluidStack(Fluids.LAVA, 32_000),
            IFluidHandler.FluidAction.EXECUTE,
        )
        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())
        source.ioController.setNetworkTarget(provider.target)

        assertTrue(source.hasStoredContents())

        val dropped = ItemStack(BufferRegistries.item.get())
        source.saveContentsToItem(dropped, registries)
        val blockEntityData = requireNotNull(dropped.get(DataComponents.BLOCK_ENTITY_DATA))

        val restored = newBuffer()
        restored.loadWithComponents(blockEntityData.copyTag(), registries)

        assertEquals(180, restored.getItemCount(0))
        assertEquals(32_000, restored.getFluid(0).amount)
        assertEquals(IoMode.PASSIVE, restored.ioController.mode)
        assertEquals(null, restored.ioController.target)
        assertTrue(restored.ioController.configuration.isDefault)
    }

    @Test
    fun `empty buffer reports no stored contents so its drop stays stackable`() {
        val source = newBuffer()
        source.ioController.setNetworkTarget(FakeNetworkOutputProvider(FakeNetworkStorage()).target)

        assertFalse(source.hasStoredContents())
    }

    private fun newBuffer(): BufferBlockEntity =
        BufferBlockEntity(
            BlockPos.ZERO,
            BufferRegistries.block.get().defaultBlockState(),
        )

    private fun storeEntry(
        slot: Int,
        variant: CompoundTag,
        amount: Long,
    ) = CompoundTag().apply {
        putInt("slot", slot)
        put("variant", variant)
        putLong("amount", amount)
    }
}
