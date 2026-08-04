package rhx.lazy.integration.jade.mysticalagriculture

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.mysticalagriculture.EssenceConverterBlockEntity
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.StreamServerDataProvider

internal object JadeEssenceConverterIntegration {
    fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(
            EssenceConverterJadeDataProvider,
            EssenceConverterBlockEntity::class.java,
        )
    }
}

internal data class EssenceConverterJadeData(
    val targetTier: String,
    val outputCount: Long,
    val remainderUnits: Int,
    val outputMode: String,
)

internal object EssenceConverterJadeDataProvider :
    StreamServerDataProvider<BlockAccessor, EssenceConverterJadeData> {
    override fun streamData(accessor: BlockAccessor): EssenceConverterJadeData? {
        val blockEntity = accessor.blockEntity as? EssenceConverterBlockEntity ?: return null
        return EssenceConverterJadeData(
            targetTier = blockEntity.targetTier?.serializedName.orEmpty(),
            outputCount = blockEntity.outputCount,
            remainderUnits = blockEntity.remainderUnits,
            outputMode =
                if (blockEntity.isNetworkOutputPaused) {
                    NETWORK_PAUSED_MODE
                } else {
                    blockEntity.currentOutputMode.name.lowercase()
                },
        )
    }

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, EssenceConverterJadeData> = EssenceConverterJadeDataCodec

    override fun getUid(): ResourceLocation = JadeProviderIds.essenceConverter
}

private const val NETWORK_PAUSED_MODE = "network_paused"

private object EssenceConverterJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, EssenceConverterJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: EssenceConverterJadeData,
    ) {
        buffer.writeUtf(value.targetTier)
        buffer.writeVarLong(value.outputCount)
        buffer.writeVarInt(value.remainderUnits)
        buffer.writeUtf(value.outputMode)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): EssenceConverterJadeData =
        EssenceConverterJadeData(
            targetTier = buffer.readUtf(),
            outputCount = buffer.readVarLong(),
            remainderUnits = buffer.readVarInt(),
            outputMode = buffer.readUtf(),
        )
}
