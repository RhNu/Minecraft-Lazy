package rhx.lazy.feature.shaping

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.IoCapabilityRegistration

@EventBusSubscriber(modid = MOD_ID)
internal object ShaperCapabilities {
    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        IoCapabilityRegistration.items(
            event,
            ShaperRegistries.blockEntity.get(),
            { it.inputHandler },
            { it.outputHandler },
        )
    }
}
