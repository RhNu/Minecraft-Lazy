package rhx.lazy.integration.jade.mysticalagriculture.client

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.mysticalagriculture.EssenceConverterJadeDataProvider
import rhx.lazy.integration.mysticalagriculture.EssenceConverterBlock
import rhx.lazy.integration.mysticalagriculture.EssenceTier
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.config.IPluginConfig

internal object JadeEssenceConverterClientIntegration {
    fun register(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(EssenceConverterJadeComponentProvider, EssenceConverterBlock::class.java)
    }
}

private object EssenceConverterJadeComponentProvider : IBlockComponentProvider {
    override fun appendTooltip(
        tooltip: ITooltip,
        accessor: BlockAccessor,
        config: IPluginConfig,
    ) {
        val data = EssenceConverterJadeDataProvider.decodeFromData(accessor).orElse(null) ?: return
        val target =
            if (data.targetTier.isEmpty()) {
                Component.translatable("gui.lazy.essence_converter.target.unset")
            } else {
                EssenceTier.fromSerializedName(data.targetTier)?.createStack()?.hoverName
                    ?: Component.literal(data.targetTier)
            }
        tooltip.add(Component.translatable("jade.lazy.essence_converter.target", target))
        tooltip.add(Component.translatable("jade.lazy.essence_converter.output", data.outputCount))
        tooltip.add(Component.translatable("jade.lazy.essence_converter.remainder", data.remainderUnits))
        tooltip.add(
            Component.translatable(
                "jade.lazy.essence_converter.output_mode",
                Component.translatable(
                    if (data.outputMode == "network_paused") {
                        "gui.lazy.io.network_paused"
                    } else {
                        "gui.lazy.io.route.${data.outputMode}"
                    },
                ),
            ),
        )
    }

    override fun getUid(): ResourceLocation = JadeProviderIds.essenceConverter
}
