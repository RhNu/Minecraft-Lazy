package rhx.lazy.integration.beyonddimensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID

internal class GuardedNetworkStoragePort(
    modId: String,
    private val delegate: BeyondDimensionsStoragePort,
) : BeyondDimensionsStoragePort {
    private val logger = LogManager.getLogger("$MOD_ID/$modId")

    override fun primaryNetwork(player: ServerPlayer) = guarded { primaryNetwork(player) }

    override fun insertItems(
        networkId: BeyondDimensionsNetworkId,
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ) = guardedMutation(simulate) { insertItems(networkId, template, amount, simulate) }

    override fun insertFluid(
        networkId: BeyondDimensionsNetworkId,
        template: FluidStack,
        amount: Long,
        simulate: Boolean,
    ) = guardedMutation(simulate) { insertFluid(networkId, template, amount, simulate) }

    override fun insertEnergy(
        networkId: BeyondDimensionsNetworkId,
        amount: Long,
        simulate: Boolean,
    ) = guardedMutation(simulate) { insertEnergy(networkId, amount, simulate) }

    private inline fun <T> guardedMutation(
        simulate: Boolean,
        operation: BeyondDimensionsStoragePort.() -> BeyondDimensionsStorageResult<T>,
    ): BeyondDimensionsStorageResult<T> =
        guarded(
            failureResult =
                if (simulate) {
                    BeyondDimensionsStorageResult.Failed
                } else {
                    BeyondDimensionsStorageResult.OutcomeUnknown
                },
            operation = operation,
        )

    private inline fun <T> guarded(
        failureResult: BeyondDimensionsStorageResult<Nothing> = BeyondDimensionsStorageResult.Failed,
        operation: BeyondDimensionsStoragePort.() -> BeyondDimensionsStorageResult<T>,
    ): BeyondDimensionsStorageResult<T> =
        try {
            delegate.operation()
        } catch (error: LinkageError) {
            logger.error("Beyond Dimensions integration linkage failed", error)
            failureResult
        } catch (exception: RuntimeException) {
            logger.error("Beyond Dimensions integration operation failed", exception)
            failureResult
        }
}
