package rhx.lazy.integration.botanypots.client

import net.neoforged.neoforge.client.event.EntityRenderersEvent
import rhx.lazy.integration.ClientIntegrationContext
import rhx.lazy.integration.botanypots.PlanterRegistries

internal object BotanyPotsClientAdapter {
    fun initialize(context: ClientIntegrationContext) {
        context.modBus.addListener(::registerRenderers)
    }

    private fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(PlanterRegistries.blockEntity.get(), ::PlanterBlockEntityRenderer)
    }
}
