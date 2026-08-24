package rhx.lazy.integration.beyonddimensions

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

internal object BeyondDimensionsStorageAdapter : BeyondDimensionsStoragePort {
    override fun primaryNetwork(player: ServerPlayer): BeyondDimensionsStorageResult<BeyondDimensionsNetworkId> {
        val network =
            DimensionsNet.getPrimaryNetFromPlayer(player)
                ?: return BeyondDimensionsStorageResult.NetworkNotFound
        return BeyondDimensionsStorageResult.Success(BeyondDimensionsNetworkId(network.id))
    }

    override fun insertItems(
        networkId: BeyondDimensionsNetworkId,
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ): BeyondDimensionsStorageResult<Long> {
        if (template.isEmpty || amount <= 0L) return BeyondDimensionsStorageResult.Success(0L)
        val storage = storage(networkId) ?: return BeyondDimensionsStorageResult.NetworkNotFound
        return BeyondDimensionsStorageResult.Success(storage.insert(ItemStackKey(template), amount, simulate).amount())
    }

    override fun insertFluid(
        networkId: BeyondDimensionsNetworkId,
        template: FluidStack,
        amount: Long,
        simulate: Boolean,
    ): BeyondDimensionsStorageResult<Long> {
        if (template.isEmpty || amount <= 0L) return BeyondDimensionsStorageResult.Success(0L)
        val storage = storage(networkId) ?: return BeyondDimensionsStorageResult.NetworkNotFound
        return BeyondDimensionsStorageResult.Success(storage.insert(FluidStackKey(template), amount, simulate).amount())
    }

    override fun insertEnergy(
        networkId: BeyondDimensionsNetworkId,
        amount: Long,
        simulate: Boolean,
    ): BeyondDimensionsStorageResult<Long> {
        if (amount <= 0L) return BeyondDimensionsStorageResult.Success(0L)
        val storage = storage(networkId) ?: return BeyondDimensionsStorageResult.NetworkNotFound
        return BeyondDimensionsStorageResult.Success(storage.insert(EnergyStackKey.INSTANCE, amount, simulate).amount())
    }

    private fun storage(networkId: BeyondDimensionsNetworkId) =
        DimensionsNet
            .getNetFromId(networkId.value)
            ?.unifiedStorage
}
