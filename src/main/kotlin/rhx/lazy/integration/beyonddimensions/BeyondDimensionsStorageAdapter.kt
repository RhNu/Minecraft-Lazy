package rhx.lazy.integration.beyonddimensions

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult

internal object BeyondDimensionsStorageAdapter : NetworkStoragePort {
    override val isAvailable: Boolean = true

    override fun primaryNetwork(player: ServerPlayer): NetworkStorageResult<NetworkStorageId> {
        val network = DimensionsNet.getPrimaryNetFromPlayer(player) ?: return NetworkStorageResult.NetworkNotFound
        return NetworkStorageResult.Success(NetworkStorageId(network.id))
    }

    override fun itemAmount(
        networkId: NetworkStorageId,
        stack: ItemStack,
    ): NetworkStorageResult<Long> {
        if (stack.isEmpty) return NetworkStorageResult.Success(0L)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        return NetworkStorageResult.Success(storage.getStackByKey(ItemStackKey(stack)).amount())
    }

    override fun insertItem(
        networkId: NetworkStorageId,
        stack: ItemStack,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> {
        if (stack.isEmpty) return NetworkStorageResult.Success(ItemStack.EMPTY)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        val remainder = storage.insert(ItemStackKey(stack), stack.count.toLong(), simulate).amount()
        return NetworkStorageResult.Success(stack.copyWithAmount(remainder))
    }

    override fun insertItemAmount(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> {
        if (template.isEmpty || amount <= 0L) return NetworkStorageResult.Success(0L)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        return NetworkStorageResult.Success(storage.insert(ItemStackKey(template), amount, simulate).amount())
    }

    override fun extractItem(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> {
        if (template.isEmpty || amount <= 0) return NetworkStorageResult.Success(ItemStack.EMPTY)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        val extracted = storage.extract(ItemStackKey(template), amount.toLong(), simulate, false).amount()
        return NetworkStorageResult.Success(template.copyWithAmount(extracted))
    }

    override fun fluidAmount(
        networkId: NetworkStorageId,
        stack: FluidStack,
    ): NetworkStorageResult<Long> {
        if (stack.isEmpty) return NetworkStorageResult.Success(0L)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        return NetworkStorageResult.Success(storage.getStackByKey(FluidStackKey(stack)).amount())
    }

    override fun insertFluid(
        networkId: NetworkStorageId,
        stack: FluidStack,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> {
        if (stack.isEmpty) return NetworkStorageResult.Success(FluidStack.EMPTY)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        val remainder = storage.insert(FluidStackKey(stack), stack.amount.toLong(), simulate).amount()
        return NetworkStorageResult.Success(stack.copyWithAmount(remainder))
    }

    override fun extractFluid(
        networkId: NetworkStorageId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> {
        if (template.isEmpty || amount <= 0) return NetworkStorageResult.Success(FluidStack.EMPTY)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        val extracted = storage.extract(FluidStackKey(template), amount.toLong(), simulate, false).amount()
        return NetworkStorageResult.Success(template.copyWithAmount(extracted))
    }

    override fun energyAmount(networkId: NetworkStorageId): NetworkStorageResult<Long> {
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        return NetworkStorageResult.Success(storage.getStackByKey(EnergyStackKey.INSTANCE).amount())
    }

    override fun insertEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> {
        if (amount <= 0L) return NetworkStorageResult.Success(0L)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        return NetworkStorageResult.Success(storage.insert(EnergyStackKey.INSTANCE, amount, simulate).amount())
    }

    override fun extractEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> {
        if (amount <= 0L) return NetworkStorageResult.Success(0L)
        val storage = storage(networkId) ?: return NetworkStorageResult.NetworkNotFound
        return NetworkStorageResult.Success(storage.extract(EnergyStackKey.INSTANCE, amount, simulate, false).amount())
    }

    private fun storage(networkId: NetworkStorageId) =
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
