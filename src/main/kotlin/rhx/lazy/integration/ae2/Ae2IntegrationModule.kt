package rhx.lazy.integration.ae2

import appeng.api.features.GridLinkables
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object Ae2IntegrationModule : IntegrationModule {
    override val modId: String = "ae2"

    override fun initialize(context: IntegrationContext) {
        Ae2Translations.register()
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
