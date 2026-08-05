package rhx.lazy.core.io

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.testing.FakeNetworkOutputProvider
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NetworkOutputRouterTest {
    @Test
    fun `capability identity is its stable resource id`() {
        val alias =
            NetworkInsertCapability(
                NetworkInsertCapabilities.ITEM.id,
                net.minecraft.network.chat.Component
                    .literal("Different label"),
            )

        assertEquals(NetworkInsertCapabilities.ITEM, alias)
        assertTrue(alias in NetworkInsertCapabilities.all)
    }

    @Test
    fun `missing provider is a retryable failure`() {
        val target =
            NetworkTargetRef(
                ResourceLocation.fromNamespaceAndPath("lazy", "absent_provider"),
                CompoundTag(),
            )

        assertSame(
            NetworkTransferResult.TemporarilyUnavailable,
            NetworkOutputRouter.insert(target, NetworkPayload.Energy(1), simulate = false),
        )
    }

    @Test
    fun `temporarily missing capability is a retryable failure`() {
        val provider =
            FakeNetworkOutputProvider(
                FakeNetworkStorage(),
                capabilities = setOf(NetworkInsertCapabilities.ITEM),
            )

        assertSame(
            NetworkTransferResult.TemporarilyUnavailable,
            NetworkOutputRouter.insert(provider.target, NetworkPayload.Energy(1), simulate = false),
        )
    }

    @Test
    fun `invalid target data is distinguished from a missing network`() {
        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())

        assertSame(
            NetworkTransferResult.InvalidTarget,
            NetworkOutputRouter.insert(
                NetworkTargetRef(provider.id, CompoundTag()),
                NetworkPayload.Items(ItemStack(Items.STONE), 1),
                simulate = false,
            ),
        )
    }

    @Test
    fun `simulation and long remainder are preserved`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = Int.MAX_VALUE.toLong() + 5 }
        val provider = FakeNetworkOutputProvider(storage)
        val amount = Int.MAX_VALUE.toLong() + 20
        val payload = NetworkPayload.Items(ItemStack(Items.STONE), amount)

        assertEquals(
            NetworkTransferResult.Success(15),
            NetworkOutputRouter.insert(provider.target, payload, simulate = true),
        )
        assertEquals(0, storage.storedItemAmount)
        assertEquals(
            NetworkTransferResult.Success(15),
            NetworkOutputRouter.insert(provider.target, payload, simulate = false),
        )
        assertEquals(Int.MAX_VALUE.toLong() + 5, storage.storedItemAmount)
    }
}
