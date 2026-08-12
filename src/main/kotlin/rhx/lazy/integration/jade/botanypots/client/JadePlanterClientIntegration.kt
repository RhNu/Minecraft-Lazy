package rhx.lazy.integration.jade.botanypots.client

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.integration.botanypots.PlanterBlock
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.botanypots.PlanterJadeDataProvider
import rhx.lazy.integration.jade.botanypots.PlanterJadeOutputMode
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.config.IPluginConfig
import java.text.DecimalFormat

internal object JadePlanterClientIntegration {
    fun register(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(PlanterJadeComponentProvider, PlanterBlock::class.java)
    }
}

private object PlanterJadeComponentProvider : IBlockComponentProvider {
    override fun appendTooltip(
        tooltip: ITooltip,
        accessor: BlockAccessor,
        config: IPluginConfig,
    ) {
        val data = PlanterJadeDataProvider.decodeFromData(accessor).orElse(null) ?: return
        if (data.progressPercent >= 0) {
            tooltip.add(Component.translatable("jade.lazy.planter.growth", data.progressPercent))
        }
        if (data.outputEfficiency >= 0f) {
            tooltip.add(
                Component.translatable(
                    "jade.lazy.planter.output_efficiency",
                    outputEfficiencyFormat.format(data.outputEfficiency),
                ),
            )
        }
        tooltip.add(
            Component.translatable(
                "jade.lazy.planter.output_mode",
                data.outputMode.asComponent(data.networkProviderId),
            ),
        )
    }

    override fun getUid(): ResourceLocation = JadeProviderIds.planter

    private val outputEfficiencyFormat = DecimalFormat("0.##")
}

private fun PlanterJadeOutputMode.asComponent(networkProviderId: String): Component =
    when (this) {
        PlanterJadeOutputMode.PASSIVE -> Component.translatable("jade.lazy.planter.mode.passive")
        PlanterJadeOutputMode.DOWNWARD -> Component.translatable("jade.lazy.planter.mode.downward")
        PlanterJadeOutputMode.NETWORK ->
            Component.translatable(
                "jade.lazy.planter.mode.network",
                networkProviderName(networkProviderId),
            )
    }

private fun networkProviderName(providerId: String): Component {
    val resourceId = ResourceLocation.tryParse(providerId) ?: return Component.translatable("gui.lazy.io.route.network")
    return NetworkOutputProviders.get(resourceId)?.displayName
        ?: Component.literal(providerId)
}
