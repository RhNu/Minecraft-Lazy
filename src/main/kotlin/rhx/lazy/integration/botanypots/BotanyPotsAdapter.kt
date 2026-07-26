package rhx.lazy.integration.botanypots

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import rhx.lazy.core.registry.LazyCreativeTabRegistry
import rhx.lazy.integration.IntegrationContext

internal object BotanyPotsAdapter {
    fun initialize(context: IntegrationContext) {
        PlanterRegistries.register(context.modBus)
        PlanterTranslations.register()
        context.modBus.addListener(PlanterCapabilities::registerCapabilities)
        context.modBus.addListener(PlanterDataGeneration::gatherData)
        context.modBus.addListener(::addCreativeTabContents)
    }

    private fun addCreativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == LazyCreativeTabRegistry.tab.key) {
            event.accept(PlanterRegistries.item.get())
        }
    }
}
