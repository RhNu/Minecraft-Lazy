package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.fluids.capability.templates.FluidTank
import net.neoforged.neoforge.items.ItemStackHandler
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IoConfigurationTest {
    @Test
    fun `configuration round trip preserves all face modes`() {
        val configured =
            IoConfiguration(
                mode = IoMode.FACE,
                sides = RelativeSide.entries.associateWith { SideIoMode.entries[it.ordinal % SideIoMode.entries.size] },
                autoEject = true,
            )

        assertEquals(configured, IoConfiguration.load(configured.save()))
    }

    @Test
    fun `relative sides rotate with machine front`() {
        val state =
            EnergyRegistries.sourceBlock
                .get()
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)

        assertEquals(Direction.EAST, RelativeSide.FRONT.toWorldDirection(state))
        assertEquals(Direction.NORTH, RelativeSide.LEFT.toWorldDirection(state))
        assertEquals(Direction.SOUTH, RelativeSide.RIGHT.toWorldDirection(state))
        assertEquals(Direction.WEST, RelativeSide.BACK.toWorldDirection(state))
    }

    @Test
    fun `machine adapters expose unified capabilities and default to passive`() {
        val energy = EnergySourceBlockEntity(BlockPos.ZERO, EnergyRegistries.sourceBlock.get().defaultBlockState())
        val buffer = BufferBlockEntity(BlockPos.ZERO, BufferRegistries.block.get().defaultBlockState())

        assertEquals(IoMode.PASSIVE, energy.ioController.mode)
        assertEquals(setOf(ResourceKinds.ENERGY), energy.ioController.capabilities)
        assertEquals(
            setOf(ResourceKinds.ITEM, ResourceKinds.FLUID),
            buffer.ioController.capabilities,
        )
        assertTrue(buffer.ioController.sideMode(Direction.UP).allowsInput)
        assertTrue(buffer.ioController.sideMode(Direction.UP).allowsOutput)
    }

    @Test
    fun `configuration card stores a complete independent template`() {
        val stack = ItemStack(ConfigurationCardRegistries.item.get())
        val target =
            NetworkTargetRef(
                ResourceLocation.fromNamespaceAndPath("lazy", "test_card"),
                CompoundTag().apply { putInt("value", 42) },
            )
        val configuration =
            IoConfiguration(
                mode = IoMode.NETWORK,
                sides = RelativeSide.entries.associateWith { SideIoMode.BOTH },
                autoEject = true,
                networkTarget = target,
            )

        ConfigurationCardData.set(stack, configuration)
        target.data.putInt("value", 0)

        assertEquals(
            42,
            ConfigurationCardData
                .get(stack)
                .networkTarget
                ?.data
                ?.getInt("value"),
        )
    }

    @Test
    fun `machines copy a network target instead of aliasing the caller's tag`() {
        val provider = ItemOnlyProvider("aliasing")
        NetworkOutputProviders.register(provider)
        val buffer = BufferBlockEntity(BlockPos.ZERO, BufferRegistries.block.get().defaultBlockState())
        val target = provider.target()

        assertTrue(buffer.ioController.setNetworkTarget(target))
        target.data.putString("opaque", "mutated")

        assertEquals(
            "original",
            buffer.ioController.target
                ?.data
                ?.getString("opaque"),
        )
    }

    @Test
    fun `a card bound to an unusable network is rejected instead of half applied`() {
        val provider = ItemOnlyProvider("rejection")
        NetworkOutputProviders.register(provider)
        val energy = EnergySourceBlockEntity(BlockPos.ZERO, EnergyRegistries.sourceBlock.get().defaultBlockState())

        val rejected =
            energy.ioController.applyConfiguration(
                IoConfiguration(mode = IoMode.NETWORK, networkTarget = provider.target()),
            )

        assertFalse(rejected)
        assertEquals(IoMode.PASSIVE, energy.ioController.mode)
    }

    @Test
    fun `only a customised configuration reaches disk`() {
        val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        val energy = EnergySourceBlockEntity(BlockPos.ZERO, EnergyRegistries.sourceBlock.get().defaultBlockState())

        assertFalse(energy.saveWithoutMetadata(registries).contains(IO_CONFIGURATION_TAG))

        energy.ioController.setMode(IoMode.FACE)

        assertTrue(energy.saveWithoutMetadata(registries).contains(IO_CONFIGURATION_TAG))
    }

    @Test
    fun `item capability enforces input and output directions`() {
        val input = ItemStackHandler(1)
        val output = ItemStackHandler(1).apply { setStackInSlot(0, ItemStack(Items.DIAMOND, 4)) }
        var mode = SideIoMode.INPUT
        val handler = IoItemHandler(input, output) { mode }

        assertTrue(handler.insertItem(0, ItemStack(Items.IRON_INGOT), false).isEmpty)
        assertTrue(handler.extractItem(1, 1, false).isEmpty)

        mode = SideIoMode.OUTPUT
        assertEquals(Items.DIAMOND, handler.extractItem(1, 1, false).item)
        assertEquals(1, handler.insertItem(0, ItemStack(Items.GOLD_INGOT), false).count)
    }

    @Test
    fun `fluid capability enforces input and output directions`() {
        val input = FluidTank(1_000)
        val output = FluidTank(1_000).apply { fluid = FluidStack(net.minecraft.world.level.material.Fluids.WATER, 500) }
        var mode = SideIoMode.INPUT
        val handler = IoFluidHandler(input, output) { mode }

        assertEquals(250, handler.fill(FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 250), IFluidHandler.FluidAction.EXECUTE))
        assertTrue(handler.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty)

        mode = SideIoMode.OUTPUT
        assertEquals(100, handler.drain(100, IFluidHandler.FluidAction.EXECUTE).amount)
        assertEquals(0, handler.fill(FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE))
    }

    /** Accepts items only, so an energy-only machine can never bind to it. */
    private class ItemOnlyProvider(
        suffix: String,
    ) : NetworkOutputProvider {
        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lazy", "test_item_only_$suffix")
        override val displayName: Component = Component.literal("Item only $suffix")
        override val capabilities = setOf(ResourceKinds.ITEM)

        override fun icon() = ItemStack(Items.CHEST)

        override fun resolvePrimaryTarget(player: ServerPlayer) = NetworkTargetResolution.Unavailable

        override fun isTargetValid(target: NetworkTargetRef) = target.providerId == id

        override fun offer(
            target: NetworkTargetRef,
            amount: ResourceAmount<out ResourceVariant>,
            simulate: Boolean,
        ): TransferResult = TransferResult.Accepted(0)

        fun target() = NetworkTargetRef(id, CompoundTag().apply { putString("opaque", "original") })
    }

    private companion object {
        /** Mirrors the persistence key written by [IoManagedBlockEntity]. */
        const val IO_CONFIGURATION_TAG = "lazyIoConfiguration"
    }
}
