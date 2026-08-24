package rhx.lazy

import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import rhx.lazy.client.LazyRuntimeClient
import rhx.lazy.generated.integration.client.GeneratedClientIntegrationCatalog
import rhx.lazy.integration.api.IntegrationClientContext
import rhx.lazy.integration.api.IntegrationModSet
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(value = MOD_ID, dist = [Dist.CLIENT])
public object LazyClient {
    init {
        val context =
            IntegrationClientContext(
                ModLoadingContext.get().activeContainer,
                MOD_BUS,
                NeoForge.EVENT_BUS,
            )
        LazyRuntimeClient.install(context)
        GeneratedClientIntegrationCatalog.install(context, IntegrationModSet.loadedMods)
    }
}
