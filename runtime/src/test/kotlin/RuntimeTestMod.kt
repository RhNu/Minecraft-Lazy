package rhx.lazy

import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import rhx.lazy.integration.api.IntegrationCommonContext
import rhx.lazy.integration.api.IntegrationConfigContext
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/** Minimal registry bootstrap for runtime tests; optional integrations are intentionally absent. */
@Mod("lazy_test")
internal object RuntimeTestMod {
    init {
        val modContainer = ModLoadingContext.get().activeContainer
        LazyRuntime.registerConfig(IntegrationConfigContext(modContainer))
        LazyRuntime.install(IntegrationCommonContext(modContainer, MOD_BUS, NeoForge.EVENT_BUS))
    }
}
