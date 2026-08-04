package rhx.lazy.integration.beyonddimensions

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GuardedNetworkStoragePortTest {
    @Test
    fun `runtime failures become explicit results without disabling the provider`() {
        val delegate =
            object : NetworkStoragePort by FakeNetworkStorage() {
                override fun itemAmount(
                    networkId: NetworkStorageId,
                    stack: ItemStack,
                ): NetworkStorageResult<Long> = throw IllegalStateException("upstream failure")
            }
        val guarded = GuardedNetworkStoragePort("test", delegate)

        repeat(2) {
            assertSame(
                NetworkStorageResult.Failed,
                guarded.itemAmount(FakeNetworkStorage.TEST_NETWORK_ID, ItemStack(Items.STONE)),
            )
        }
        assertTrue(guarded.isAvailable)
    }

    @Test
    fun `linkage failures also become explicit results`() {
        val delegate =
            object : NetworkStoragePort by FakeNetworkStorage() {
                override fun energyAmount(networkId: NetworkStorageId): NetworkStorageResult<Long> =
                    throw NoClassDefFoundError("missing.Api")
            }
        val guarded = GuardedNetworkStoragePort("test", delegate)

        assertSame(NetworkStorageResult.Failed, guarded.energyAmount(FakeNetworkStorage.TEST_NETWORK_ID))
        assertTrue(guarded.isAvailable)
    }

    @Test
    fun `failed mutations report an unknown outcome unless simulated`() {
        val delegate =
            object : NetworkStoragePort by FakeNetworkStorage() {
                override fun insertItemAmount(
                    networkId: NetworkStorageId,
                    template: ItemStack,
                    amount: Long,
                    simulate: Boolean,
                ): NetworkStorageResult<Long> = throw IllegalStateException("failure after possible commit")
            }
        val guarded = GuardedNetworkStoragePort("test", delegate)

        assertSame(
            NetworkStorageResult.OutcomeUnknown,
            guarded.insertItemAmount(
                FakeNetworkStorage.TEST_NETWORK_ID,
                ItemStack(Items.STONE),
                64,
                simulate = false,
            ),
        )
        assertSame(
            NetworkStorageResult.Failed,
            guarded.insertItemAmount(
                FakeNetworkStorage.TEST_NETWORK_ID,
                ItemStack(Items.STONE),
                64,
                simulate = true,
            ),
        )
    }

    @Test
    fun `bulk item insertion preserves long quantities and simulation`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = Int.MAX_VALUE.toLong() + 10L }
        val amount = Int.MAX_VALUE.toLong() + 20L

        assertEquals(
            NetworkStorageResult.Success(10L),
            storage.insertItemAmount(
                FakeNetworkStorage.TEST_NETWORK_ID,
                ItemStack(Items.STONE),
                amount,
                simulate = true,
            ),
        )
        assertEquals(0L, storage.storedItemAmount)
        assertEquals(
            NetworkStorageResult.Success(10L),
            storage.insertItemAmount(
                FakeNetworkStorage.TEST_NETWORK_ID,
                ItemStack(Items.STONE),
                amount,
                simulate = false,
            ),
        )
        assertEquals(Int.MAX_VALUE.toLong() + 10L, storage.storedItemAmount)
    }
}
