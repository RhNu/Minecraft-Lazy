package rhx.lazy.integration.jade.mysticalagriculture.client

import net.minecraft.network.chat.Component
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.client.IoMachineJadeComponentProvider
import rhx.lazy.integration.jade.mysticalagriculture.EssenceConverterJadeData
import rhx.lazy.integration.jade.mysticalagriculture.EssenceConverterJadeDataProvider
import rhx.lazy.integration.mysticalagriculture.EssenceConverterBlock
import rhx.lazy.integration.mysticalagriculture.EssenceTier
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration

internal object JadeEssenceConverterClientIntegration {
    fun register(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(EssenceConverterJadeComponentProvider, EssenceConverterBlock::class.java)
    }
}

private object EssenceConverterJadeComponentProvider :
    IoMachineJadeComponentProvider<EssenceConverterJadeData>(
        EssenceConverterJadeDataProvider,
        JadeProviderIds.essenceConverter,
    ) {
    override fun appendBeforeOutput(
        tooltip: ITooltip,
        data: EssenceConverterJadeData,
    ) {
        val target =
            if (data.targetTier.isEmpty()) {
                Component.translatable("gui.lazy.essence_converter.target.unset")
            } else {
                EssenceTier.fromSerializedName(data.targetTier)?.createStack()?.hoverName
                    ?: Component.literal(data.targetTier)
            }
        tooltip.add(Component.translatable("jade.lazy.essence_converter.target", target))
        tooltip.add(
            Component.translatable(
                "jade.lazy.essence_converter.contents",
                data.outputCount,
                data.remainderUnits,
            ),
        )
    }
}
