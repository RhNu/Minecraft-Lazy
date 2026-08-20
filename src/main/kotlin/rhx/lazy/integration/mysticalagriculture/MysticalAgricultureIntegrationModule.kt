package rhx.lazy.integration.mysticalagriculture

import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import rhx.lazy.core.registry.LazyCreativeTabRegistry
import rhx.lazy.feature.simulation.SimulationRegistries
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object MysticalAgricultureIntegrationModule : IntegrationModule {
    override val modId: String = "mysticalagriculture"

    override fun initialize(context: IntegrationContext) {
        MysticalAgricultureSimulationAdapter.register()
        EssenceConverterRegistries.register(context.modBus)
        EssenceConverterTranslations.register()
        context.modBus.addListener(EssenceConverterCapabilities::registerCapabilities)
        context.modBus.addListener(EssenceConverterDataGeneration::gatherData)
        context.modBus.addListener(::addCreativeTabContents)
    }

    private fun addCreativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == LazyCreativeTabRegistry.tab.key) {
            event.insertAfter(
                ItemStack(SimulationRegistries.item.get()),
                ItemStack(EssenceConverterRegistries.item.get()),
                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS,
            )
        }
    }
}
