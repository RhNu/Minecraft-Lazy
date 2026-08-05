package rhx.lazy.integration.botanypots

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.io.IoRoute
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.NetworkTransferResult
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanterOutputRouterTest {
    @Test
    fun `passive route moves pending products into internal output slots`() {
        val pending = mutableListOf(ItemStack(Items.DIAMOND, 20))
        val outputs = MutableList(PlanterOutputRouter.OUTPUT_SLOT_COUNT) { ItemStack.EMPTY }
        var outputDirtyCalls = 0
        var pendingDirtyCalls = 0
        val router =
            createRouter(
                pending = pending,
                outputs = outputs,
                markOutputsDirty = { outputDirtyCalls++ },
                markPendingDirty = { pendingDirtyCalls++ },
            )

        assertEquals(rhx.lazy.core.io.IoPushResult.Success, router.route(null, IoRoute.PASSIVE, null))

        assertTrue(pending.isEmpty())
        assertEquals(20, outputs.single { !it.isEmpty }.count)
        assertEquals(1, outputDirtyCalls)
        assertEquals(1, pendingDirtyCalls)
    }

    @Test
    fun `output handler restores a stack after a slot implementation temporarily clears it`() {
        val outputs = MutableList(PlanterOutputRouter.OUTPUT_SLOT_COUNT) { ItemStack.EMPTY }
        val router = createRouter(outputs = outputs)
        val original = ItemStack(Items.DIAMOND, 20)

        router.outputHandler.setStackInSlot(0, original)
        val stored = router.outputHandler.getStackInSlot(0)
        router.outputHandler.setStackInSlot(0, ItemStack.EMPTY)
        router.outputHandler.setStackInSlot(0, stored)

        assertTrue(ItemStack.matches(original, outputs[0]))
    }

    @Test
    fun `unchanged pending output does not mark persistence dirty`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = 0 }
        val pending = mutableListOf(ItemStack(Items.DIAMOND, 20))
        var dirtyCalls = 0
        val router = createRouter(pending = pending, markPendingDirty = { dirtyCalls++ })
        val provider = registerProvider(storage)

        routeNetwork(router, provider)

        assertEquals(0, dirtyCalls)
        assertEquals(20, pending.single().count)
    }

    @Test
    fun `partially forwarded pending output is marked dirty once`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = 8 }
        val pending = mutableListOf(ItemStack(Items.DIAMOND, 20))
        var dirtyCalls = 0
        val router = createRouter(pending = pending, markPendingDirty = { dirtyCalls++ })
        val provider = registerProvider(storage)

        routeNetwork(router, provider)

        assertEquals(1, dirtyCalls)
        assertEquals(12, pending.single().count)
        assertEquals(8, storage.storedItemAmount)
    }

    private fun createRouter(
        pending: MutableList<ItemStack> = mutableListOf(),
        markOutputsDirty: () -> Unit = {},
        markPendingDirty: () -> Unit = {},
        outputs: MutableList<ItemStack> = MutableList(PlanterOutputRouter.OUTPUT_SLOT_COUNT) { ItemStack.EMPTY },
    ): PlanterOutputRouter =
        PlanterOutputRouter(
            blockPos = BlockPos.ZERO,
            outputs = outputs,
            pendingDrops = pending,
            markOutputsDirty = markOutputsDirty,
            markPendingDirty = markPendingDirty,
        )

    private fun routeNetwork(
        router: PlanterOutputRouter,
        provider: TestProvider,
    ) {
        router.routeNetwork(provider.target)
    }

    private fun registerProvider(storage: FakeNetworkStorage): TestProvider {
        val provider = TestProvider(storage)
        NetworkOutputProviders.register(provider)
        return provider
    }

    private class TestProvider(
        private val storage: FakeNetworkStorage,
    ) : NetworkOutputProvider {
        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath("lazy", "test_planter_${nextId++}")
        override val displayName: Component = Component.literal("Test planter")
        override val capabilities = setOf(NetworkInsertCapabilities.ITEM)
        val target = NetworkTargetRef(id, CompoundTag())

        override fun icon(): ItemStack = ItemStack(Items.CHEST)

        override fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution = NetworkTargetResolution.Unavailable

        override fun isTargetValid(target: NetworkTargetRef): Boolean = target.providerId == id

        override fun insert(
            target: NetworkTargetRef,
            payload: NetworkPayload,
            simulate: Boolean,
        ): NetworkTransferResult {
            val items = payload as NetworkPayload.Items
            return storage.insertItemAmount(items.template, items.amount, simulate)
        }

        companion object {
            private var nextId = 0
        }
    }
}
