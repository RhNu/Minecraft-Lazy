package rhx.lazy

import net.neoforged.fml.ModList
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import rhx.lazy.generated.integration.catalog.GeneratedCommonIntegrationCatalog
import rhx.lazy.integration.api.IntegrationCommonContext
import rhx.lazy.integration.api.IntegrationConfigContext
import rhx.lazy.integration.api.IntegrationModSet
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(MOD_ID)
public object Lazy {
    init {
        val modContainer = ModLoadingContext.get().activeContainer
        val loadedMods = ModList.get().mods.mapTo(mutableSetOf()) { modInfo -> modInfo.modId }
        IntegrationModSet.install(loadedMods)

        val configContext = IntegrationConfigContext(modContainer)
        LazyRuntime.registerConfig(configContext)
        GeneratedCommonIntegrationCatalog.registerConfig(configContext, loadedMods)

        val commonContext = IntegrationCommonContext(modContainer, MOD_BUS, NeoForge.EVENT_BUS)
        LazyRuntime.install(commonContext)
        GeneratedCommonIntegrationCatalog.install(commonContext, loadedMods)
    }
}
