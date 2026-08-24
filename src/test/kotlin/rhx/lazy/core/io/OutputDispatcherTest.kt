package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.core.resource.itemAmount
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class OutputDispatcherTest {
    @Test
    fun `network cursor rotates between stored identities`() {
        val store = ResourceStore(ItemResourceKind, 2, 100)
        store.insert(requireNotNull(itemAmount(ItemStack(Items.STONE), 20)))
        store.insert(requireNotNull(itemAmount(ItemStack(Items.DIAMOND), 20)))
        val source = StoredOutputSource(listOf(store))
        val provider = RecordingProvider()
        val dispatcher = OutputDispatcher(BlockPos.ZERO) { true }

        dispatcher.pushToNetwork(source, provider.target, TransferBudget(1))
        dispatcher.pushToNetwork(source, provider.target, TransferBudget(1))

        assertEquals(listOf(Items.STONE, Items.DIAMOND), provider.items)
        assertEquals(19L, store.amount(0))
        assertEquals(19L, store.amount(1))
    }

    @Test
    fun `unknown network outcome never decrements the source`() {
        val store = ResourceStore(ItemResourceKind, 1, 100)
        store.insert(requireNotNull(itemAmount(ItemStack(Items.STONE), 20)))
        val provider = RecordingProvider(TransferResult.OutcomeUnknown)

        val result =
            OutputDispatcher(BlockPos.ZERO) { true }
                .pushToNetwork(StoredOutputSource(listOf(store)), provider.target, TransferBudget(64))

        assertEquals(IoPushResult.OutcomeUnknown, result)
        assertEquals(20L, store.amount(0))
    }

    private class RecordingProvider(
        var result: TransferResult = TransferResult.Accepted(1),
    ) : NetworkOutputProvider {
        override val id = ResourceLocation.fromNamespaceAndPath("lazy", "dispatcher_test_${IDS.getAndIncrement()}")
        override val displayName: Component = Component.literal("dispatcher test")
        override val capabilities = setOf(ResourceKinds.ITEM)
        val target = NetworkTargetRef(id, CompoundTag())
        val items = mutableListOf<net.minecraft.world.item.Item>()

        init {
            NetworkOutputProviders.register(this)
        }

        override fun icon(): ItemStack = ItemStack(Items.CHEST)

        override fun resolvePrimaryTarget(player: ServerPlayer) = NetworkTargetResolution.NotFound

        override fun isTargetValid(target: NetworkTargetRef): Boolean = target.providerId == id

        override fun offer(
            target: NetworkTargetRef,
            amount: ResourceAmount<out ResourceVariant>,
            simulate: Boolean,
        ): TransferResult {
            val variant = amount.variant as ItemVariant
            items += variant.template.item
            return result
        }

        private companion object {
            val IDS = AtomicInteger()
        }
    }
}
