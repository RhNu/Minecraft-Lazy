package rhx.lazy.integration.jade.botanypots

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
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

internal data class PlanterJadeData(
    val progressPercent: Int,
    val pendingProducts: Boolean,
    val downwardOutput: Boolean,
    val networkForwarding: Boolean,
)

internal object PlanterJadeDataProvider : StreamServerDataProvider<BlockAccessor, PlanterJadeData> {
    override fun streamData(accessor: BlockAccessor): PlanterJadeData? {
        val blockEntity = accessor.blockEntity as? PlanterBlockEntity ?: return null
        val requiredGrowthTicks = blockEntity.requiredGrowthTicks()
        return PlanterJadeData(
            progressPercent =
                if (requiredGrowthTicks > 0) {
                    (blockEntity.progress() * PERCENT_DENOMINATOR).roundToInt().coerceIn(0, PERCENT_DENOMINATOR)
                } else {
                    NO_RECIPE_PROGRESS
                },
            pendingProducts = blockEntity.hasPendingDrops,
            downwardOutput = blockEntity.isDownwardOutputEnabled,
            networkForwarding = blockEntity.isNetworkForwardingEnabled,
        )
    }

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, PlanterJadeData> = PlanterJadeDataCodec

    override fun getUid(): ResourceLocation = JadeProviderIds.planter

    private const val PERCENT_DENOMINATOR = 100
    internal const val NO_RECIPE_PROGRESS = -1
}

private object PlanterJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, PlanterJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: PlanterJadeData,
    ) {
        buffer.writeVarInt(value.progressPercent + PROGRESS_OFFSET)
        buffer.writeBoolean(value.pendingProducts)
        buffer.writeBoolean(value.downwardOutput)
        buffer.writeBoolean(value.networkForwarding)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): PlanterJadeData =
        PlanterJadeData(
            progressPercent = buffer.readVarInt() - PROGRESS_OFFSET,
            pendingProducts = buffer.readBoolean(),
            downwardOutput = buffer.readBoolean(),
            networkForwarding = buffer.readBoolean(),
        )

    private const val PROGRESS_OFFSET = 1
}
