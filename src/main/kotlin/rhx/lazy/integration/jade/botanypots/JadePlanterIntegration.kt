package rhx.lazy.integration.jade.botanypots

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import rhx.lazy.core.io.IoRoute
import rhx.lazy.integration.botanypots.PlanterBlockEntity
import rhx.lazy.integration.jade.JadeProviderIds
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.StreamServerDataProvider
import kotlin.math.roundToInt

internal object JadePlanterIntegration {
    fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(PlanterJadeDataProvider, PlanterBlockEntity::class.java)
    }
}

internal enum class PlanterJadeOutputMode {
    PASSIVE,
    DOWNWARD,
    NETWORK,
}

internal data class PlanterJadeData(
    val progressPercent: Int,
    val outputEfficiency: Float,
    val outputMode: PlanterJadeOutputMode,
    val networkProviderId: String,
)

internal object PlanterJadeDataProvider : StreamServerDataProvider<BlockAccessor, PlanterJadeData> {
    override fun streamData(accessor: BlockAccessor): PlanterJadeData? {
        val blockEntity = accessor.blockEntity as? PlanterBlockEntity ?: return null
        val requiredGrowthTicks = blockEntity.requiredGrowthTicks()
        val outputMode = blockEntity.ioController.route.toPlanterJadeOutputMode()
        return PlanterJadeData(
            progressPercent =
                if (requiredGrowthTicks > 0) {
                    (blockEntity.progress() * PERCENT_DENOMINATOR).roundToInt().coerceIn(0, PERCENT_DENOMINATOR)
                } else {
                    NO_RECIPE_PROGRESS
                },
            outputEfficiency = blockEntity.outputEfficiency(),
            outputMode = outputMode,
            networkProviderId =
                if (outputMode == PlanterJadeOutputMode.NETWORK) {
                    blockEntity.ioController.target
                        ?.providerId
                        ?.toString()
                        .orEmpty()
                } else {
                    ""
                },
        )
    }

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, PlanterJadeData> = PlanterJadeDataCodec

    override fun getUid(): ResourceLocation = JadeProviderIds.planter

    private const val PERCENT_DENOMINATOR = 100
    internal const val NO_RECIPE_PROGRESS = -1
}

private fun IoRoute.toPlanterJadeOutputMode(): PlanterJadeOutputMode =
    when (this) {
        IoRoute.PASSIVE -> PlanterJadeOutputMode.PASSIVE
        IoRoute.DOWNWARD -> PlanterJadeOutputMode.DOWNWARD
        IoRoute.NETWORK -> PlanterJadeOutputMode.NETWORK
        IoRoute.ADJACENT -> PlanterJadeOutputMode.PASSIVE
    }

private object PlanterJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, PlanterJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: PlanterJadeData,
    ) {
        buffer.writeVarInt(value.progressPercent + PROGRESS_OFFSET)
        buffer.writeFloat(value.outputEfficiency)
        buffer.writeVarInt(value.outputMode.ordinal)
        buffer.writeUtf(value.networkProviderId)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): PlanterJadeData {
        val progressPercent = buffer.readVarInt() - PROGRESS_OFFSET
        val outputEfficiency = buffer.readFloat()
        val outputMode = PlanterJadeOutputMode.entries.getOrNull(buffer.readVarInt()) ?: PlanterJadeOutputMode.PASSIVE
        return PlanterJadeData(
            progressPercent = progressPercent,
            outputEfficiency = outputEfficiency,
            outputMode = outputMode,
            networkProviderId = buffer.readUtf(),
        )
    }

    private const val PROGRESS_OFFSET = 1
}
