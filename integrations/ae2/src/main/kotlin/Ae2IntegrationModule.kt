package rhx.lazy.integration.ae2

import appeng.api.features.GridLinkables
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object Ae2IntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        AeStoragePayloadAdapters.registerAe2Adapters()
        NetworkOutputProviders.register(Ae2NetworkOutputProvider)
        context.modBus.addListener(::commonSetup)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            GridLinkables.register(ConfigurationCardRegistries.item.get(), ConfigurationCardLinking.handler)
        }
    }
}
