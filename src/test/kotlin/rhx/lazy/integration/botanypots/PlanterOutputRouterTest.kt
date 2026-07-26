package rhx.lazy.integration.botanypots

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanterOutputRouterTest {
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
        val router =
            createRouter(
                storage = storage,
                pending = pending,
                markPendingDirty = { dirtyCalls++ },
            )

        router.forwardPendingToNetwork()

        assertEquals(0, dirtyCalls)
        assertEquals(20, pending.single().count)
    }

    @Test
    fun `partially forwarded pending output is marked dirty once`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = 8 }
        val pending = mutableListOf(ItemStack(Items.DIAMOND, 20))
        var dirtyCalls = 0
        val router =
            createRouter(
                storage = storage,
                pending = pending,
                markPendingDirty = { dirtyCalls++ },
            )

        router.forwardPendingToNetwork()

        assertEquals(1, dirtyCalls)
        assertEquals(12, pending.single().count)
        assertEquals(8, storage.storedItemAmount)
    }

    private fun createRouter(
        storage: FakeNetworkStorage = FakeNetworkStorage(),
        pending: MutableList<ItemStack> = mutableListOf(),
        markPendingDirty: () -> Unit = {},
        outputs: MutableList<ItemStack> =
            MutableList(PlanterOutputRouter.OUTPUT_SLOT_COUNT) {
                ItemStack.EMPTY
            },
    ): PlanterOutputRouter =
        PlanterOutputRouter(
            blockPos = BlockPos.ZERO,
            outputs = outputs,
            pendingDrops = pending,
            networkStorage = storage,
            networkId = { FakeNetworkStorage.TEST_NETWORK_ID },
            disableNetworkForwarding = {},
            markOutputsDirty = {},
            markPendingDirty = markPendingDirty,
        )
}
