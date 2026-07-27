package rhx.lazy.integration.jade.botanypots.client

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import rhx.lazy.integration.botanypots.PlanterBlock
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.botanypots.PlanterJadeDataProvider
import rhx.lazy.integration.jade.client.enabledState
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.config.IPluginConfig

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
        if (data.pendingProducts) {
            tooltip.add(Component.translatable("jade.lazy.planter.pending"))
        }
        tooltip.add(
            Component.translatable(
                "jade.lazy.planter.outputs",
                enabledState(data.downwardOutput),
                enabledState(data.networkForwarding),
            ),
        )
    }

    override fun getUid(): ResourceLocation = JadeProviderIds.planter
}
