package rhx.lazy.integration.jade.mysticalagriculture

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import rhx.lazy.integration.jade.IoMachineJadeData
import rhx.lazy.integration.jade.IoMachineJadeDataProvider
import rhx.lazy.integration.jade.JadeOutputState
import rhx.lazy.integration.jade.JadeOutputStateCodec
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.MachineStorageHiders
import rhx.lazy.integration.mysticalagriculture.EssenceConverterBlock
import rhx.lazy.integration.mysticalagriculture.EssenceConverterBlockEntity
import snownee.jade.api.IWailaCommonRegistration

internal object JadeEssenceConverterIntegration {
    fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(
            EssenceConverterJadeDataProvider,
            EssenceConverterBlockEntity::class.java,
        )
        registration.registerItemStorage(MachineStorageHiders.essenceConverterItems, EssenceConverterBlock::class.java)
    }
}

internal data class EssenceConverterJadeData(
    val targetTier: String,
    val outputCount: Long,
    val remainderUnits: Int,
    override val output: JadeOutputState,
) : IoMachineJadeData

internal object EssenceConverterJadeDataProvider :
    IoMachineJadeDataProvider<EssenceConverterBlockEntity, EssenceConverterJadeData>(
        EssenceConverterBlockEntity::class.java,
        JadeProviderIds.essenceConverter,
        EssenceConverterJadeDataCodec,
    ) {
    override fun createData(
        entity: EssenceConverterBlockEntity,
        output: JadeOutputState,
    ): EssenceConverterJadeData =
        EssenceConverterJadeData(
            targetTier = entity.targetTier?.serializedName.orEmpty(),
            outputCount = entity.outputCount,
            remainderUnits = entity.remainderUnits,
            output = output,
        )
}

private object EssenceConverterJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, EssenceConverterJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: EssenceConverterJadeData,
    ) {
        buffer.writeUtf(value.targetTier)
        buffer.writeVarLong(value.outputCount)
        buffer.writeVarInt(value.remainderUnits)
        JadeOutputStateCodec.encode(buffer, value.output)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): EssenceConverterJadeData =
        EssenceConverterJadeData(
            targetTier = buffer.readUtf(),
            outputCount = buffer.readVarLong(),
            remainderUnits = buffer.readVarInt(),
            output = JadeOutputStateCodec.decode(buffer),
        )
}
