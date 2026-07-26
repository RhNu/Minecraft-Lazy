package rhx.lazy.core.storage

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

@JvmInline
internal value class NetworkStorageId(
    val value: Int,
) {
    init {
        require(value >= 0) { "Network storage id must not be negative" }
    }
}

internal sealed interface NetworkStorageResult<out T> {
    data class Success<T>(
        val value: T,
    ) : NetworkStorageResult<T>

    data object Unavailable : NetworkStorageResult<Nothing>

    data object NetworkNotFound : NetworkStorageResult<Nothing>

    data object Failed : NetworkStorageResult<Nothing>
}

internal interface NetworkStoragePort {
    val isAvailable: Boolean

    fun primaryNetwork(player: ServerPlayer): NetworkStorageResult<NetworkStorageId>

    fun itemAmount(
        networkId: NetworkStorageId,
        stack: ItemStack,
    ): NetworkStorageResult<Long>

    fun insertItem(
        networkId: NetworkStorageId,
        stack: ItemStack,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack>

    fun extractItem(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack>

    fun fluidAmount(
        networkId: NetworkStorageId,
        stack: FluidStack,
    ): NetworkStorageResult<Long>

    fun insertFluid(
        networkId: NetworkStorageId,
        stack: FluidStack,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack>

    fun extractFluid(
        networkId: NetworkStorageId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack>

    fun energyAmount(networkId: NetworkStorageId): NetworkStorageResult<Long>

    fun insertEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long>

    fun extractEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long>
}

internal open class NetworkStorageService : NetworkStoragePort {
    private var delegate: NetworkStoragePort = UnavailableNetworkStorage

    override val isAvailable: Boolean
        get() = delegate.isAvailable

    fun install(provider: NetworkStoragePort) {
        check(delegate === UnavailableNetworkStorage) { "A network storage provider is already installed" }
        check(provider.isAvailable) { "Cannot install an unavailable network storage provider" }
        delegate = provider
    }

    override fun primaryNetwork(player: ServerPlayer): NetworkStorageResult<NetworkStorageId> = delegate.primaryNetwork(player)

    override fun itemAmount(
        networkId: NetworkStorageId,
        stack: ItemStack,
    ): NetworkStorageResult<Long> = delegate.itemAmount(networkId, stack)

    override fun insertItem(
        networkId: NetworkStorageId,
        stack: ItemStack,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> = delegate.insertItem(networkId, stack, simulate)

    override fun extractItem(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> = delegate.extractItem(networkId, template, amount, simulate)

    override fun fluidAmount(
        networkId: NetworkStorageId,
        stack: FluidStack,
    ): NetworkStorageResult<Long> = delegate.fluidAmount(networkId, stack)

    override fun insertFluid(
        networkId: NetworkStorageId,
        stack: FluidStack,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> = delegate.insertFluid(networkId, stack, simulate)

    override fun extractFluid(
        networkId: NetworkStorageId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> = delegate.extractFluid(networkId, template, amount, simulate)

    override fun energyAmount(networkId: NetworkStorageId): NetworkStorageResult<Long> = delegate.energyAmount(networkId)

    override fun insertEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> = delegate.insertEnergy(networkId, amount, simulate)

    override fun extractEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> = delegate.extractEnergy(networkId, amount, simulate)
}

internal object NetworkStorage : NetworkStorageService()

private object UnavailableNetworkStorage : NetworkStoragePort {
    override val isAvailable: Boolean = false

    override fun primaryNetwork(player: ServerPlayer) = unavailable<NetworkStorageId>()

    override fun itemAmount(
        networkId: NetworkStorageId,
        stack: ItemStack,
    ) = unavailable<Long>()

    override fun insertItem(
        networkId: NetworkStorageId,
        stack: ItemStack,
        simulate: Boolean,
    ) = unavailable<ItemStack>()

    override fun extractItem(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ) = unavailable<ItemStack>()

    override fun fluidAmount(
        networkId: NetworkStorageId,
        stack: FluidStack,
    ) = unavailable<Long>()

    override fun insertFluid(
        networkId: NetworkStorageId,
        stack: FluidStack,
        simulate: Boolean,
    ) = unavailable<FluidStack>()

    override fun extractFluid(
        networkId: NetworkStorageId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ) = unavailable<FluidStack>()

    override fun energyAmount(networkId: NetworkStorageId) = unavailable<Long>()

    override fun insertEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ) = unavailable<Long>()

    override fun extractEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ) = unavailable<Long>()

    private fun <T> unavailable(): NetworkStorageResult<T> = NetworkStorageResult.Unavailable
}
