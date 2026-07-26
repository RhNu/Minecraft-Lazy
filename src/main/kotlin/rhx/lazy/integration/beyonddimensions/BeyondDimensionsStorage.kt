package rhx.lazy.integration.beyonddimensions

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

internal object BeyondDimensionsStorage : DimensionNetworkStorage {
    override val isAvailable: Boolean = true

    override fun primaryNetwork(player: ServerPlayer): DimensionNetworkResult<DimensionNetworkId> {
        val network = DimensionsNet.getPrimaryNetFromPlayer(player) ?: return DimensionNetworkResult.NetworkNotFound
        return DimensionNetworkResult.Success(DimensionNetworkId(network.id))
    }

    override fun itemAmount(
        networkId: DimensionNetworkId,
        stack: ItemStack,
    ): DimensionNetworkResult<Long> {
        if (stack.isEmpty) return DimensionNetworkResult.Success(0L)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        return DimensionNetworkResult.Success(storage.getStackByKey(ItemStackKey(stack)).amount())
    }

    override fun insertItem(
        networkId: DimensionNetworkId,
        stack: ItemStack,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack> {
        if (stack.isEmpty) return DimensionNetworkResult.Success(ItemStack.EMPTY)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        val remainder = storage.insert(ItemStackKey(stack), stack.count.toLong(), simulate).amount()
        return DimensionNetworkResult.Success(stack.copyWithAmount(remainder))
    }

    override fun extractItem(
        networkId: DimensionNetworkId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack> {
        if (template.isEmpty || amount <= 0) return DimensionNetworkResult.Success(ItemStack.EMPTY)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        val extracted = storage.extract(ItemStackKey(template), amount.toLong(), simulate, false).amount()
        return DimensionNetworkResult.Success(template.copyWithAmount(extracted))
    }

    override fun fluidAmount(
        networkId: DimensionNetworkId,
        stack: FluidStack,
    ): DimensionNetworkResult<Long> {
        if (stack.isEmpty) return DimensionNetworkResult.Success(0L)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        return DimensionNetworkResult.Success(storage.getStackByKey(FluidStackKey(stack)).amount())
    }

    override fun insertFluid(
        networkId: DimensionNetworkId,
        stack: FluidStack,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack> {
        if (stack.isEmpty) return DimensionNetworkResult.Success(FluidStack.EMPTY)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        val remainder = storage.insert(FluidStackKey(stack), stack.amount.toLong(), simulate).amount()
        return DimensionNetworkResult.Success(stack.copyWithAmount(remainder))
    }

    override fun extractFluid(
        networkId: DimensionNetworkId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack> {
        if (template.isEmpty || amount <= 0) return DimensionNetworkResult.Success(FluidStack.EMPTY)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        val extracted = storage.extract(FluidStackKey(template), amount.toLong(), simulate, false).amount()
        return DimensionNetworkResult.Success(template.copyWithAmount(extracted))
    }

    override fun energyAmount(networkId: DimensionNetworkId): DimensionNetworkResult<Long> {
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        return DimensionNetworkResult.Success(storage.getStackByKey(EnergyStackKey.INSTANCE).amount())
    }

    override fun insertEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long> {
        if (amount <= 0L) return DimensionNetworkResult.Success(0L)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        return DimensionNetworkResult.Success(storage.insert(EnergyStackKey.INSTANCE, amount, simulate).amount())
    }

    override fun extractEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long> {
        if (amount <= 0L) return DimensionNetworkResult.Success(0L)
        val storage = storage(networkId) ?: return DimensionNetworkResult.NetworkNotFound
        return DimensionNetworkResult.Success(storage.extract(EnergyStackKey.INSTANCE, amount, simulate, false).amount())
    }

    private fun storage(networkId: DimensionNetworkId) =
        DimensionsNet
            .getNetFromId(networkId.value)
            ?.unifiedStorage
}

private fun ItemStack.copyWithAmount(amount: Long): ItemStack =
    if (amount <= 0L) {
        ItemStack.EMPTY
    } else {
        copyWithCount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

private fun FluidStack.copyWithAmount(amount: Long): FluidStack =
    if (amount <= 0L) {
        FluidStack.EMPTY
    } else {
        copyWithAmount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }
