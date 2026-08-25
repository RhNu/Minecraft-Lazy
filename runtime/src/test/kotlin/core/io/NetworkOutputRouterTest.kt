package rhx.lazy.core.io

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.resource.ResourceKinds
import rhx.lazy.core.resource.energyAmount
import rhx.lazy.core.resource.itemAmount
import rhx.lazy.core.testing.FakeNetworkOutputProvider
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NetworkOutputRouterTest {
    @Test
    fun `resource kinds expose stable ids`() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("lazy", "item"), ResourceKinds.ITEM.id)
        assertTrue(ResourceKinds.ITEM in ResourceKinds.all)
    }

    @Test
    fun `missing provider is a retryable failure`() {
        val target =
            NetworkTargetRef(
                ResourceLocation.fromNamespaceAndPath("lazy", "absent_provider"),
                CompoundTag(),
            )

        assertSame(
            TransferResult.TemporarilyUnavailable,
            NetworkOutputRouter.offer(target, requireNotNull(energyAmount(1)), simulate = false),
        )
    }

    @Test
    fun `temporarily missing capability is a retryable failure`() {
        val provider =
            FakeNetworkOutputProvider(
                FakeNetworkStorage(),
                capabilities = setOf(ResourceKinds.ITEM),
            )

        assertSame(
            TransferResult.TemporarilyUnavailable,
            NetworkOutputRouter.offer(provider.target, requireNotNull(energyAmount(1)), simulate = false),
        )
    }

    @Test
    fun `invalid target data is distinguished from a missing network`() {
        val provider = FakeNetworkOutputProvider(FakeNetworkStorage())

        assertSame(
            TransferResult.InvalidTarget,
            NetworkOutputRouter.offer(
                NetworkTargetRef(provider.id, CompoundTag()),
                requireNotNull(itemAmount(ItemStack(Items.STONE), 1)),
                simulate = false,
            ),
        )
    }

    @Test
    fun `simulation and long remainder are preserved`() {
        val storage = FakeNetworkStorage().apply { itemCapacity = Int.MAX_VALUE.toLong() + 5 }
        val provider = FakeNetworkOutputProvider(storage)
        val amount = Int.MAX_VALUE.toLong() + 20
        val payload = requireNotNull(itemAmount(ItemStack(Items.STONE), amount))

        assertEquals(
            TransferResult.Accepted(Int.MAX_VALUE.toLong() + 5),
            NetworkOutputRouter.offer(provider.target, payload, simulate = true),
        )
        assertEquals(0, storage.storedItemAmount)
        assertEquals(
            TransferResult.Accepted(Int.MAX_VALUE.toLong() + 5),
            NetworkOutputRouter.offer(provider.target, payload, simulate = false),
        )
        assertEquals(Int.MAX_VALUE.toLong() + 5, storage.storedItemAmount)
    }
}
