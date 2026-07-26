package rhx.lazy.integration.beyonddimensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList
import net.neoforged.neoforge.fluids.FluidStack
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID

internal object BeyondDimensionsIntegration : DimensionNetworkStorage {
    private val logger = LogManager.getLogger("$MOD_ID/BeyondDimensionsIntegration")
    private var delegate: DimensionNetworkStorage = UnavailableDimensionNetworkStorage

    override val isAvailable: Boolean
        get() = delegate.isAvailable

    fun init() {
        if (!ModList.get().isLoaded(BEYOND_DIMENSIONS_MOD_ID)) return

        try {
            delegate = BeyondDimensionsStorage
            logger.info("Enabled Beyond Dimensions integration")
        } catch (error: LinkageError) {
            logger.error("Failed to link Beyond Dimensions integration", error)
        }
    }

    override fun primaryNetwork(player: ServerPlayer): DimensionNetworkResult<DimensionNetworkId> = guarded { primaryNetwork(player) }

    override fun itemAmount(
        networkId: DimensionNetworkId,
        stack: ItemStack,
    ): DimensionNetworkResult<Long> = guarded { itemAmount(networkId, stack) }

    override fun insertItem(
        networkId: DimensionNetworkId,
        stack: ItemStack,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack> = guarded { insertItem(networkId, stack, simulate) }

    override fun extractItem(
        networkId: DimensionNetworkId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<ItemStack> = guarded { extractItem(networkId, template, amount, simulate) }

    override fun fluidAmount(
        networkId: DimensionNetworkId,
        stack: FluidStack,
    ): DimensionNetworkResult<Long> = guarded { fluidAmount(networkId, stack) }

    override fun insertFluid(
        networkId: DimensionNetworkId,
        stack: FluidStack,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack> = guarded { insertFluid(networkId, stack, simulate) }

    override fun extractFluid(
        networkId: DimensionNetworkId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): DimensionNetworkResult<FluidStack> = guarded { extractFluid(networkId, template, amount, simulate) }

    override fun energyAmount(networkId: DimensionNetworkId): DimensionNetworkResult<Long> = guarded { energyAmount(networkId) }

    override fun insertEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long> = guarded { insertEnergy(networkId, amount, simulate) }

    override fun extractEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ): DimensionNetworkResult<Long> = guarded { extractEnergy(networkId, amount, simulate) }

    private inline fun <T> guarded(operation: DimensionNetworkStorage.() -> DimensionNetworkResult<T>): DimensionNetworkResult<T> {
        val current = delegate
        if (!current.isAvailable) return DimensionNetworkResult.IntegrationUnavailable

        return try {
            current.operation()
        } catch (error: LinkageError) {
            delegate = UnavailableDimensionNetworkStorage
            logger.error("Beyond Dimensions integration became unavailable", error)
            DimensionNetworkResult.IntegrationUnavailable
        } catch (exception: RuntimeException) {
            logger.error("Beyond Dimensions operation failed", exception)
            DimensionNetworkResult.Failed
        }
    }

    private const val BEYOND_DIMENSIONS_MOD_ID = "beyonddimensions"
}

private object UnavailableDimensionNetworkStorage : DimensionNetworkStorage {
    override val isAvailable: Boolean = false

    override fun primaryNetwork(player: ServerPlayer) = unavailable<DimensionNetworkId>()

    override fun itemAmount(
        networkId: DimensionNetworkId,
        stack: ItemStack,
    ) = unavailable<Long>()

    override fun insertItem(
        networkId: DimensionNetworkId,
        stack: ItemStack,
        simulate: Boolean,
    ) = unavailable<ItemStack>()

    override fun extractItem(
        networkId: DimensionNetworkId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ) = unavailable<ItemStack>()

    override fun fluidAmount(
        networkId: DimensionNetworkId,
        stack: FluidStack,
    ) = unavailable<Long>()

    override fun insertFluid(
        networkId: DimensionNetworkId,
        stack: FluidStack,
        simulate: Boolean,
    ) = unavailable<FluidStack>()

    override fun extractFluid(
        networkId: DimensionNetworkId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ) = unavailable<FluidStack>()

    override fun energyAmount(networkId: DimensionNetworkId) = unavailable<Long>()

    override fun insertEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ) = unavailable<Long>()

    override fun extractEnergy(
        networkId: DimensionNetworkId,
        amount: Long,
        simulate: Boolean,
    ) = unavailable<Long>()

    private fun <T> unavailable(): DimensionNetworkResult<T> = DimensionNetworkResult.IntegrationUnavailable
}
