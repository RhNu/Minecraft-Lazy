package rhx.lazy.core.testing

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult
import kotlin.math.min

internal class FakeNetworkStorage(
    override var isAvailable: Boolean = true,
) : NetworkStoragePort {
    var networkExists = true
    var itemCapacity = Long.MAX_VALUE
    var fluidCapacity = Long.MAX_VALUE
    var energyCapacity = Long.MAX_VALUE

    var storedItem = ItemStack.EMPTY
    var storedItemAmount = 0L
    var storedFluid = FluidStack.EMPTY
    var storedFluidAmount = 0L
    var storedEnergy = 0L

    override fun primaryNetwork(player: ServerPlayer): NetworkStorageResult<NetworkStorageId> =
        if (networkExists) {
            NetworkStorageResult.Success(TEST_NETWORK_ID)
        } else {
            NetworkStorageResult.NetworkNotFound
        }

    override fun itemAmount(
        networkId: NetworkStorageId,
        stack: ItemStack,
    ): NetworkStorageResult<Long> = withNetwork { storedItemAmount }

    override fun insertItem(
        networkId: NetworkStorageId,
        stack: ItemStack,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> =
        withNetwork {
            val accepted = min(stack.count.toLong(), (itemCapacity - storedItemAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0L) {
                storedItem = stack.copyWithCount(1)
                storedItemAmount += accepted
            }
            stack.copyWithAmount(stack.count.toLong() - accepted)
        }

    override fun extractItem(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> =
        withNetwork {
            val extracted = min(amount.toLong().coerceAtLeast(0L), storedItemAmount)
            if (!simulate) storedItemAmount -= extracted
            template.copyWithAmount(extracted)
        }

    override fun fluidAmount(
        networkId: NetworkStorageId,
        stack: FluidStack,
    ): NetworkStorageResult<Long> = withNetwork { storedFluidAmount }

    override fun insertFluid(
        networkId: NetworkStorageId,
        stack: FluidStack,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> =
        withNetwork {
            val accepted = min(stack.amount.toLong(), (fluidCapacity - storedFluidAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0L) {
                storedFluid = stack.copyWithAmount(1)
                storedFluidAmount += accepted
            }
            stack.copyWithAmount(stack.amount.toLong() - accepted)
        }

    override fun extractFluid(
        networkId: NetworkStorageId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> =
        withNetwork {
            val extracted = min(amount.toLong().coerceAtLeast(0L), storedFluidAmount)
            if (!simulate) storedFluidAmount -= extracted
            template.copyWithAmount(extracted)
        }

    override fun energyAmount(networkId: NetworkStorageId): NetworkStorageResult<Long> = withNetwork { storedEnergy }

    override fun insertEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> =
        withNetwork {
            val accepted = min(amount.coerceAtLeast(0L), (energyCapacity - storedEnergy).coerceAtLeast(0L))
            if (!simulate) storedEnergy += accepted
            amount.coerceAtLeast(0L) - accepted
        }

    override fun extractEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> =
        withNetwork {
            val extracted = min(amount.coerceAtLeast(0L), storedEnergy)
            if (!simulate) storedEnergy -= extracted
            extracted
        }

    private fun <T> withNetwork(value: () -> T): NetworkStorageResult<T> =
        when {
            !isAvailable -> NetworkStorageResult.Unavailable
            !networkExists -> NetworkStorageResult.NetworkNotFound
            else -> NetworkStorageResult.Success(value())
        }

    companion object {
        val TEST_NETWORK_ID = NetworkStorageId(7)
    }
}

private fun ItemStack.copyWithAmount(amount: Long): ItemStack =
    if (amount <= 0L) ItemStack.EMPTY else copyWithCount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

private fun FluidStack.copyWithAmount(amount: Long): FluidStack =
    if (amount <= 0L) FluidStack.EMPTY else copyWithAmount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
