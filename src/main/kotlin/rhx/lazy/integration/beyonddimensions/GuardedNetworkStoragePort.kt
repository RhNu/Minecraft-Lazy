package rhx.lazy.integration.beyonddimensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult

internal class GuardedNetworkStoragePort(
    modId: String,
    private val delegate: NetworkStoragePort,
) : NetworkStoragePort {
    private val logger = LogManager.getLogger("$MOD_ID/$modId")

    override val isAvailable: Boolean
        get() = delegate.isAvailable

    override fun primaryNetwork(player: ServerPlayer): NetworkStorageResult<NetworkStorageId> = guarded { primaryNetwork(player) }

    override fun itemAmount(
        networkId: NetworkStorageId,
        stack: ItemStack,
    ): NetworkStorageResult<Long> = guarded { itemAmount(networkId, stack) }

    override fun insertItem(
        networkId: NetworkStorageId,
        stack: ItemStack,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> = guardedMutation(simulate) { insertItem(networkId, stack, simulate) }

    override fun insertItemAmount(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> = guardedMutation(simulate) { insertItemAmount(networkId, template, amount, simulate) }

    override fun extractItem(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> = guardedMutation(simulate) { extractItem(networkId, template, amount, simulate) }

    override fun fluidAmount(
        networkId: NetworkStorageId,
        stack: FluidStack,
    ): NetworkStorageResult<Long> = guarded { fluidAmount(networkId, stack) }

    override fun insertFluid(
        networkId: NetworkStorageId,
        stack: FluidStack,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> = guardedMutation(simulate) { insertFluid(networkId, stack, simulate) }

    override fun extractFluid(
        networkId: NetworkStorageId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> = guardedMutation(simulate) { extractFluid(networkId, template, amount, simulate) }

    override fun energyAmount(networkId: NetworkStorageId): NetworkStorageResult<Long> = guarded { energyAmount(networkId) }

    override fun insertEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> = guardedMutation(simulate) { insertEnergy(networkId, amount, simulate) }

    override fun extractEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> = guardedMutation(simulate) { extractEnergy(networkId, amount, simulate) }

    private inline fun <T> guardedMutation(
        simulate: Boolean,
        operation: NetworkStoragePort.() -> NetworkStorageResult<T>,
    ): NetworkStorageResult<T> =
        guarded(
            failureResult =
                if (simulate) {
                    NetworkStorageResult.Failed
                } else {
                    NetworkStorageResult.OutcomeUnknown
                },
            operation = operation,
        )

    private inline fun <T> guarded(
        failureResult: NetworkStorageResult<Nothing> = NetworkStorageResult.Failed,
        operation: NetworkStoragePort.() -> NetworkStorageResult<T>,
    ): NetworkStorageResult<T> =
        try {
            delegate.operation()
        } catch (error: LinkageError) {
            logger.error("Network storage integration linkage failed", error)
            failureResult
        } catch (exception: RuntimeException) {
            logger.error("Network storage integration operation failed", exception)
            failureResult
        }
}
