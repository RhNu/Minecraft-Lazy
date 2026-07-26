package rhx.lazy.integration.beyonddimensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import kotlin.math.min

internal class FakeDimensionNetworkStorage(
    override var isAvailable: Boolean = true,
) : DimensionNetworkStorage {
    var networkExists = true
    var itemCapacity = Long.MAX_VALUE
    var fluidCapacity = Long.MAX_VALUE
    var energyCapacity = Long.MAX_VALUE

    var storedItem = ItemStack.EMPTY
    var storedItemAmount = 0L
    var storedFluid = FluidStack.EMPTY
    var storedFluidAmount = 0L
    var storedEnergy = 0L

    override fun primaryNetwork(player: ServerPlayer): DimensionNetworkResult<DimensionNetworkId> =
        if (networkExists) {
            DimensionNetworkResult.Success(TEST_NETWORK_ID)
        } else {
            DimensionNetworkResult.NetworkNotFound
        }

    override fun itemAmount(
        networkId: DimensionNetworkId,
        stack: ItemStack,
    ): DimensionNetworkResult<Long> = withNetwork { storedItemAmount }

    override fun insertItem(
        networkId: DimensionNetworkId,
        stack: ItemStack,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack> =
        withNetwork {
            val accepted = min(stack.count.toLong(), (itemCapacity - storedItemAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0L) {
                storedItem = stack.copyWithCount(1)
                storedItemAmount += accepted
            }
            stack.copyWithAmount(stack.count.toLong() - accepted)
        }

    override fun extractItem(
        networkId: DimensionNetworkId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack> =
        withNetwork {
            val extracted = min(amount.toLong().coerceAtLeast(0L), storedItemAmount)
            if (!simulate) storedItemAmount -= extracted
            template.copyWithAmount(extracted)
        }

    override fun fluidAmount(
        networkId: DimensionNetworkId,
        stack: FluidStack,
    ): DimensionNetworkResult<Long> = withNetwork { storedFluidAmount }

    override fun insertFluid(
        networkId: DimensionNetworkId,
        stack: FluidStack,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack> =
        withNetwork {
            val accepted = min(stack.amount.toLong(), (fluidCapacity - storedFluidAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0L) {
                storedFluid = stack.copyWithAmount(1)
                storedFluidAmount += accepted
            }
            stack.copyWithAmount(stack.amount.toLong() - accepted)
        }

    override fun extractFluid(
        networkId: DimensionNetworkId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack> =
        withNetwork {
            val extracted = min(amount.toLong().coerceAtLeast(0L), storedFluidAmount)
            if (!simulate) storedFluidAmount -= extracted
            template.copyWithAmount(extracted)
        }

    override fun energyAmount(networkId: DimensionNetworkId): DimensionNetworkResult<Long> = withNetwork { storedEnergy }

    override fun insertEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long> =
        withNetwork {
            val accepted = min(amount.coerceAtLeast(0L), (energyCapacity - storedEnergy).coerceAtLeast(0L))
            if (!simulate) storedEnergy += accepted
            amount.coerceAtLeast(0L) - accepted
        }

    override fun extractEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long> =
        withNetwork {
            val extracted = min(amount.coerceAtLeast(0L), storedEnergy)
            if (!simulate) storedEnergy -= extracted
            extracted
        }

    private fun <T> withNetwork(value: () -> T): DimensionNetworkResult<T> =
        when {
            !isAvailable -> DimensionNetworkResult.IntegrationUnavailable
            !networkExists -> DimensionNetworkResult.NetworkNotFound
            else -> DimensionNetworkResult.Success(value())
        }

    companion object {
        val TEST_NETWORK_ID = DimensionNetworkId(7)
    }
}

private fun ItemStack.copyWithAmount(amount: Long): ItemStack =
    if (amount <= 0L) ItemStack.EMPTY else copyWithCount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

private fun FluidStack.copyWithAmount(amount: Long): FluidStack =
    if (amount <= 0L) FluidStack.EMPTY else copyWithAmount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
