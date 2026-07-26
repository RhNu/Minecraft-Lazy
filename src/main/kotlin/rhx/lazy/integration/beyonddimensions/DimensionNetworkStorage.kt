package rhx.lazy.integration.beyonddimensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

@JvmInline
internal value class DimensionNetworkId(
    val value: Int,
) {
    init {
        require(value >= 0) { "Dimension network id must not be negative" }
    }
}

internal sealed interface DimensionNetworkResult<out T> {
    data class Success<T>(
        val value: T,
    ) : DimensionNetworkResult<T>

    data object IntegrationUnavailable : DimensionNetworkResult<Nothing>

    data object NetworkNotFound : DimensionNetworkResult<Nothing>

    data object Failed : DimensionNetworkResult<Nothing>
}

internal interface DimensionNetworkStorage {
    val isAvailable: Boolean

    fun primaryNetwork(player: ServerPlayer): DimensionNetworkResult<DimensionNetworkId>

    fun itemAmount(
        networkId: DimensionNetworkId,
        stack: ItemStack,
    ): DimensionNetworkResult<Long>

    fun insertItem(
        networkId: DimensionNetworkId,
        stack: ItemStack,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack>

    fun extractItem(
        networkId: DimensionNetworkId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack>

    fun fluidAmount(
        networkId: DimensionNetworkId,
        stack: FluidStack,
    ): DimensionNetworkResult<Long>

    fun insertFluid(
        networkId: DimensionNetworkId,
        stack: FluidStack,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack>

    fun extractFluid(
        networkId: DimensionNetworkId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack>

    fun energyAmount(networkId: DimensionNetworkId): DimensionNetworkResult<Long>

    fun insertEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long>

    fun extractEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long>
}
