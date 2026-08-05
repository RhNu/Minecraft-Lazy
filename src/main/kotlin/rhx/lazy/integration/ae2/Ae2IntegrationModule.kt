package rhx.lazy.integration.ae2

import appeng.api.features.GridLinkables
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.registry.LazyCreativeTabRegistry
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object Ae2IntegrationModule : IntegrationModule {
    override val modId: String = "ae2"

    override fun initialize(context: IntegrationContext) {
        Ae2Registries.register(context.modBus)
        Ae2Translations.register()
        AeStoragePayloadAdapters.registerAe2Adapters()
        NetworkOutputProviders.register(Ae2NetworkOutputProvider)
        context.modBus.addListener(::commonSetup)
        context.modBus.addListener(Ae2DataGeneration::gatherData)
        context.modBus.addListener(::addCreativeTabContents)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            GridLinkables.register(Ae2Registries.meOutputLinkCard.get(), MeOutputLinkCard.LINKABLE_HANDLER)
        }
    }

    private fun addCreativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == LazyCreativeTabRegistry.tab.key) {
            event.accept(Ae2Registries.meOutputLinkCard.get())
        }
    }
}
