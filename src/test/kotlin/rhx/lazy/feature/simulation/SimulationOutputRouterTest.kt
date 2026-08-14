package rhx.lazy.feature.simulation

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.io.IoConfiguration
import rhx.lazy.core.io.IoMode
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.NetworkTransferResult
import rhx.lazy.core.storage.LongItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulationOutputRouterTest {
    @Test
    fun `passive route materializes long item quantities into sixteen normal slots`() {
        val fixture = Fixture()
        fixture.router.enqueue(ItemStack(Items.DIAMOND), 1_100)

        fixture.router.route(null, IoConfiguration.DEFAULT, emptySet())

        assertEquals(1_024, fixture.items.sumOf(ItemStack::getCount))
        assertEquals(76, fixture.pendingItems.single().count)
        assertTrue(fixture.changed)
    }

    @Test
    fun `fluid backlog safely materializes into a large tank`() {
        val fixture = Fixture()
        fixture.router.enqueue(FluidStack(Fluids.WATER, 1), 300_000)

        fixture.router.route(null, IoConfiguration.DEFAULT, emptySet())

        assertEquals(300_000, fixture.fluids.single { !it.isEmpty }.amount)
        assertTrue(fixture.pendingFluids.isEmpty())
    }

    @Test
    fun `fully accepted quantities leave no backlog`() {
        val fixture = Fixture()
        fixture.router.enqueue(ItemStack(Items.DIAMOND), 130)

        fixture.router.route(null, IoConfiguration.DEFAULT, emptySet())

        assertFalse(fixture.router.hasPending)
        assertTrue(fixture.router.hasOutputs)
        assertEquals(listOf(64, 64, 2), fixture.items.filterNot(ItemStack::isEmpty).map(ItemStack::getCount))
    }

    @Test
    fun `different fluids materialize into independent tanks`() {
        val fixture = Fixture()
        fixture.router.enqueue(FluidStack(Fluids.WATER, 1), 1_000)
        fixture.router.enqueue(FluidStack(Fluids.LAVA, 1), 2_000)

        fixture.router.route(null, IoConfiguration.DEFAULT, emptySet())

        assertEquals(listOf(1_000, 2_000), fixture.fluids.filterNot(FluidStack::isEmpty).map(FluidStack::getAmount))
        assertTrue(fixture.pendingFluids.isEmpty())
    }

    @Test
    fun `idle network route does not mark storage changed`() {
        val fixture = Fixture()
        val provider = TestProvider("idle") { NetworkTransferResult.Success(0) }
        NetworkOutputProviders.register(provider)

        assertEquals(
            IoPushResult.Success,
            fixture.router.route(
                null,
                IoConfiguration(mode = IoMode.NETWORK, networkTarget = provider.target()),
                emptySet(),
            ),
        )
        assertFalse(fixture.changed)
    }

    @Test
    fun `partial network success is marked changed before target failure`() {
        val fixture = Fixture()
        var calls = 0
        val provider =
            TestProvider("partial") { payload ->
                calls++
                if (calls == 1) {
                    NetworkTransferResult.Success((payload as NetworkPayload.Items).amount - 1)
                } else {
                    NetworkTransferResult.TargetMissing
                }
            }
        NetworkOutputProviders.register(provider)
        fixture.router.enqueue(ItemStack(Items.DIAMOND), 10)
        fixture.router.enqueue(ItemStack(Items.EMERALD), 10)
        fixture.changed = false

        assertEquals(
            IoPushResult.TargetMissing,
            fixture.router.route(
                null,
                IoConfiguration(mode = IoMode.NETWORK, networkTarget = provider.target()),
                emptySet(),
            ),
        )
        assertTrue(fixture.changed)
        assertEquals(9, fixture.pendingItems.first().count)
    }

    @Test
    fun `accumulator preserves quantities beyond one long entry`() {
        val fixture = Fixture()
        val accumulator = SimulationOutputAccumulator()
        accumulator.add(ItemStack(Items.DIAMOND), Long.MAX_VALUE)
        accumulator.add(ItemStack(Items.DIAMOND), 10)

        accumulator.flush(fixture.router)

        assertEquals(listOf(Long.MAX_VALUE, 10L), fixture.pendingItems.map { it.count })
    }

    private class Fixture {
        val items = MutableList(SimulationOutputRouter.ITEM_SLOTS) { ItemStack.EMPTY }
        val fluids = MutableList(SimulationOutputRouter.FLUID_TANKS) { FluidStack.EMPTY }
        val pendingItems = mutableListOf<LongItemStack>()
        val pendingFluids = mutableListOf<LongFluidStack>()
        var changed = false
        val router =
            SimulationOutputRouter(BlockPos.ZERO, items, fluids, pendingItems, pendingFluids) {
                changed = true
            }
    }

    private class TestProvider(
        name: String,
        private val transfer: (NetworkPayload) -> NetworkTransferResult,
    ) : NetworkOutputProvider {
        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lazy_test", name)
        override val displayName: Component = Component.literal(name)
        override val capabilities = setOf(NetworkInsertCapabilities.ITEM, NetworkInsertCapabilities.FLUID)

        override fun icon() = ItemStack(Items.CHEST)

        override fun resolvePrimaryTarget(player: net.minecraft.server.level.ServerPlayer) = NetworkTargetResolution.Unavailable

        override fun isTargetValid(target: NetworkTargetRef) = true

        override fun insert(
            target: NetworkTargetRef,
            payload: NetworkPayload,
            simulate: Boolean,
        ) = transfer(payload)

        fun target() = NetworkTargetRef(id, CompoundTag())
    }
}
