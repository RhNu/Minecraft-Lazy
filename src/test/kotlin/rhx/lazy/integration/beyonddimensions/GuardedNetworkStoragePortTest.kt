package rhx.lazy.integration.beyonddimensions

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
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
}
