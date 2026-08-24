package rhx.lazy.integration.mysticalagriculture

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.core.io.IoCapabilityRegistration

internal object EssenceConverterCapabilities {
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        IoCapabilityRegistration.items(
            event,
            EssenceConverterRegistries.blockEntity.get(),
            { it.inputHandler },
            { it.outputHandler },
        )
    }
}
