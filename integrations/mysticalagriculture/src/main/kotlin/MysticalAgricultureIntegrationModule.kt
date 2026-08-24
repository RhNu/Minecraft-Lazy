package rhx.lazy.integration.mysticalagriculture

import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import rhx.lazy.core.registry.LazyCreativeTabRegistry
import rhx.lazy.feature.simulation.SimulationRegistries
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext
import rhx.lazy.integration.api.IntegrationConfigContext

@LazyCommonEntrypoint
internal object MysticalAgricultureIntegrationModule : CommonIntegration {
    override fun registerConfig(context: IntegrationConfigContext) {
        EssenceConverterConfigs.register(context.modContainer)
    }

    override fun install(context: IntegrationCommonContext) {
        MysticalAgricultureSimulationAdapter.register()
        EssenceConverterRegistries.register(context.modBus)
        context.modBus.addListener(EssenceConverterCapabilities::registerCapabilities)
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
