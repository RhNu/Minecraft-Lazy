package rhx.lazy.integration.botanypots

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanterOutputRouterTest {
    @Test
    fun `unchanged pending output does not mark persistence dirty`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = 0 }
        val pending = mutableListOf(ItemStack(Items.DIAMOND, 20))
        var dirtyCalls = 0
        val router = createRouter(storage, pending) { dirtyCalls++ }

        router.forwardPendingToNetwork()

        assertEquals(0, dirtyCalls)
        assertEquals(20, pending.single().count)
    }

    @Test
    fun `partially forwarded pending output is marked dirty once`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = 8 }
        val pending = mutableListOf(ItemStack(Items.DIAMOND, 20))
        var dirtyCalls = 0
        val router = createRouter(storage, pending) { dirtyCalls++ }

        router.forwardPendingToNetwork()

        assertEquals(1, dirtyCalls)
        assertEquals(12, pending.single().count)
        assertEquals(8, storage.storedItemAmount)
    }

    private fun createRouter(
        storage: FakeNetworkStorage,
        pending: MutableList<ItemStack>,
        markPendingDirty: () -> Unit,
    ): PlanterOutputRouter =
        PlanterOutputRouter(
            blockPos = BlockPos.ZERO,
            outputs = MutableList(PlanterOutputRouter.OUTPUT_SLOT_COUNT) { ItemStack.EMPTY },
            pendingDrops = pending,
            networkStorage = storage,
            networkId = { FakeNetworkStorage.TEST_NETWORK_ID },
            disableNetworkForwarding = {},
            markOutputsDirty = {},
            markPendingDirty = markPendingDirty,
        )
}
