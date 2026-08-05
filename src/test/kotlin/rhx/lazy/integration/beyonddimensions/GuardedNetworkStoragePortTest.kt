package rhx.lazy.integration.beyonddimensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.fluids.FluidStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class GuardedNetworkStoragePortTest {
    @Test
    fun `runtime failures become explicit results`() {
        val delegate =
            object : TestPort() {
                override fun insertFluid(
                    networkId: BeyondDimensionsNetworkId,
                    stack: FluidStack,
                    simulate: Boolean,
                ): BeyondDimensionsStorageResult<Long> = throw IllegalStateException("upstream failure")
            }
        val guarded = GuardedNetworkStoragePort("test", delegate)

        repeat(2) {
            assertSame(
                BeyondDimensionsStorageResult.Failed,
                guarded.insertFluid(NETWORK_ID, FluidStack.EMPTY, simulate = true),
            )
        }
    }

    @Test
    fun `linkage failures also become explicit results`() {
        val delegate =
            object : TestPort() {
                override fun insertEnergy(
                    networkId: BeyondDimensionsNetworkId,
                    amount: Long,
                    simulate: Boolean,
                ): BeyondDimensionsStorageResult<Long> = throw NoClassDefFoundError("missing.Api")
            }
        val guarded = GuardedNetworkStoragePort("test", delegate)

        assertSame(
            BeyondDimensionsStorageResult.OutcomeUnknown,
            guarded.insertEnergy(NETWORK_ID, 1, simulate = false),
        )
    }

    @Test
    fun `failed mutations report an unknown outcome unless simulated`() {
        val delegate =
            object : TestPort() {
                override fun insertItems(
                    networkId: BeyondDimensionsNetworkId,
                    template: ItemStack,
                    amount: Long,
                    simulate: Boolean,
                ): BeyondDimensionsStorageResult<Long> = throw IllegalStateException("failure after possible commit")
            }
        val guarded = GuardedNetworkStoragePort("test", delegate)

        assertSame(
            BeyondDimensionsStorageResult.OutcomeUnknown,
            guarded.insertItems(NETWORK_ID, ItemStack(Items.STONE), 64, simulate = false),
        )
        assertSame(
            BeyondDimensionsStorageResult.Failed,
            guarded.insertItems(NETWORK_ID, ItemStack(Items.STONE), 64, simulate = true),
        )
    }

    @Test
    fun `bulk item insertion preserves long quantities and simulation`() {
        val capacity = Int.MAX_VALUE.toLong() + 10L
        val amount = Int.MAX_VALUE.toLong() + 20L
        var stored = 0L
        val port =
            object : TestPort() {
                override fun insertItems(
                    networkId: BeyondDimensionsNetworkId,
                    template: ItemStack,
                    amount: Long,
                    simulate: Boolean,
                ): BeyondDimensionsStorageResult<Long> {
                    val accepted = (capacity - stored).coerceAtLeast(0).coerceAtMost(amount)
                    if (!simulate) stored += accepted
                    return BeyondDimensionsStorageResult.Success(amount - accepted)
                }
            }

        assertEquals(
            BeyondDimensionsStorageResult.Success(10L),
            port.insertItems(NETWORK_ID, ItemStack(Items.STONE), amount, simulate = true),
        )
        assertEquals(0L, stored)
        assertEquals(
            BeyondDimensionsStorageResult.Success(10L),
            port.insertItems(NETWORK_ID, ItemStack(Items.STONE), amount, simulate = false),
        )
        assertEquals(capacity, stored)
    }

    private open class TestPort : BeyondDimensionsStoragePort {
        override fun primaryNetwork(player: ServerPlayer): BeyondDimensionsStorageResult<BeyondDimensionsNetworkId> =
            BeyondDimensionsStorageResult.Success(NETWORK_ID)

        override fun insertItems(
            networkId: BeyondDimensionsNetworkId,
            template: ItemStack,
            amount: Long,
            simulate: Boolean,
        ): BeyondDimensionsStorageResult<Long> = BeyondDimensionsStorageResult.Success(0L)

        override fun insertFluid(
            networkId: BeyondDimensionsNetworkId,
            stack: FluidStack,
            simulate: Boolean,
        ): BeyondDimensionsStorageResult<Long> = BeyondDimensionsStorageResult.Success(0L)

        override fun insertEnergy(
            networkId: BeyondDimensionsNetworkId,
            amount: Long,
            simulate: Boolean,
        ): BeyondDimensionsStorageResult<Long> = BeyondDimensionsStorageResult.Success(0L)
    }

    private companion object {
        val NETWORK_ID = BeyondDimensionsNetworkId(7)
    }
}
