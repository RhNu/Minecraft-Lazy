package rhx.lazy

import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import rhx.lazy.integration.LazyIntegrations
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(value = MOD_ID, dist = [Dist.CLIENT])
internal object LazyClient {
    init {
        LazyIntegrations.initializeClient(MOD_BUS, NeoForge.EVENT_BUS)
    }
}
