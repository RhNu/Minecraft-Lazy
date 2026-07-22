package rhx.compose.datagen

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.compose.MOD_ID

@EventBusSubscriber(modid = MOD_ID)
internal object DataGeneration {
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        if (event.includeServer()) {
            registerServerProviders(event)
        }
        if (event.includeClient()) {
            registerClientProviders(event)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun registerServerProviders(event: GatherDataEvent) = Unit

    @Suppress("UNUSED_PARAMETER")
    private fun registerClientProviders(event: GatherDataEvent) = Unit
}
