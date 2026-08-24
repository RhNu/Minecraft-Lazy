package rhx.lazy

import net.neoforged.fml.ModList
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import rhx.lazy.generated.integration.mysticalagriculture.MysticalagricultureIntegrationBridge
import rhx.lazy.integration.api.IntegrationCommonContext
import rhx.lazy.integration.api.IntegrationConfigContext
import rhx.lazy.integration.api.IntegrationModSet
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/** Development-only bootstrap used by the isolated DataGen run. */
@Mod(MOD_ID)
internal object DataGenMod {
    init {
        val modContainer = ModLoadingContext.get().activeContainer
        val loadedMods = ModList.get().mods.mapTo(mutableSetOf()) { modInfo -> modInfo.modId }
        IntegrationModSet.install(loadedMods)
        val mysticalAgriculture = MysticalagricultureIntegrationBridge.createCommon()

        val configContext = IntegrationConfigContext(modContainer)
        LazyRuntime.registerConfig(configContext)
        mysticalAgriculture.registerConfig(configContext)

        val commonContext = IntegrationCommonContext(modContainer, MOD_BUS, NeoForge.EVENT_BUS)
        LazyRuntime.install(commonContext)
        mysticalAgriculture.install(commonContext)
    }
}
