package rhx.lazy.core.storage

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NetworkStorageServiceTest {
    @Test
    fun `service is unavailable before a provider is installed`() {
        val service = NetworkStorageService()

        assertFalse(service.isAvailable)
        assertSame(NetworkStorageResult.Unavailable, service.energyAmount(NetworkStorageId(0)))
    }

    @Test
    fun `service installs exactly one available provider`() {
        val service = NetworkStorageService()
        val provider = FakeNetworkStorage().apply { storedEnergy = 42 }

        service.install(provider)

        assertTrue(service.isAvailable)
        assertEquals(NetworkStorageResult.Success(42L), service.energyAmount(FakeNetworkStorage.TEST_NETWORK_ID))
        val failure =
            try {
                service.install(FakeNetworkStorage())
                null
            } catch (exception: IllegalStateException) {
                exception
            }
        assertNotNull(failure)
    }

    @Test
    fun `service rejects an unavailable provider`() {
        val service = NetworkStorageService()

        val failure =
            try {
                service.install(FakeNetworkStorage(isAvailable = false))
                null
            } catch (exception: IllegalStateException) {
                exception
            }
        assertNotNull(failure)
        assertFalse(service.isAvailable)
    }

    @Test
    fun `expected missing network remains a domain result`() {
        val service = NetworkStorageService()
        val provider = FakeNetworkStorage().apply { networkExists = false }
        service.install(provider)

        assertSame(
            NetworkStorageResult.NetworkNotFound,
            service.itemAmount(FakeNetworkStorage.TEST_NETWORK_ID, ItemStack(Items.STONE)),
        )
    }
}
