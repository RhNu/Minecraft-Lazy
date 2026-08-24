package rhx.lazy.feature.buffer

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.IoCapabilityRegistration

@EventBusSubscriber(modid = MOD_ID)
internal object BufferCapabilities {
    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        val type = BufferRegistries.blockEntity.get()
        IoCapabilityRegistration.items(event, type, { it.itemHandler }, { it.itemHandler })
        IoCapabilityRegistration.fluids(event, type, { it.fluidHandler }, { it.fluidHandler })
    }
}
