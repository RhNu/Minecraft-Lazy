package rhx.lazy

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.generated.datagen.GeneratedDataGenCatalog

@EventBusSubscriber(modid = MOD_ID)
internal object DataGenerationEntrypoint {
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        GeneratedDataGenCatalog.gather(event)
    }
}
