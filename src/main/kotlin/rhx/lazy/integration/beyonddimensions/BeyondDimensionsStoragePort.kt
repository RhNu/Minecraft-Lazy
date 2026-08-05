package rhx.lazy.integration.beyonddimensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

@JvmInline
internal value class BeyondDimensionsNetworkId(
    val value: Int,
) {
    init {
        require(value >= 0) { "Beyond Dimensions network id must not be negative" }
    }
}

internal sealed interface BeyondDimensionsStorageResult<out T> {
    data class Success<T>(
        val value: T,
    ) : BeyondDimensionsStorageResult<T>

    data object NetworkNotFound : BeyondDimensionsStorageResult<Nothing>

    data object Failed : BeyondDimensionsStorageResult<Nothing>

    data object OutcomeUnknown : BeyondDimensionsStorageResult<Nothing>
}

internal interface BeyondDimensionsStoragePort {
    fun primaryNetwork(player: ServerPlayer): BeyondDimensionsStorageResult<BeyondDimensionsNetworkId>

    fun insertItems(
        networkId: BeyondDimensionsNetworkId,
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ): BeyondDimensionsStorageResult<Long>

    fun insertFluid(
        networkId: BeyondDimensionsNetworkId,
        stack: FluidStack,
        simulate: Boolean,
    ): BeyondDimensionsStorageResult<Long>

    fun insertEnergy(
        networkId: BeyondDimensionsNetworkId,
        amount: Long,
        simulate: Boolean,
    ): BeyondDimensionsStorageResult<Long>
}
