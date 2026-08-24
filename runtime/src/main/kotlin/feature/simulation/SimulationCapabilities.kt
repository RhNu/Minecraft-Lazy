package rhx.lazy.feature.simulation

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.IoCapabilityRegistration

@EventBusSubscriber(modid = MOD_ID)
internal object SimulationCapabilities {
    @SubscribeEvent
    fun register(event: RegisterCapabilitiesEvent) {
        val type = SimulationRegistries.blockEntity.get()
        IoCapabilityRegistration.items(event, type, { it.inputItemHandler }, { it.outputItemHandler })
        IoCapabilityRegistration.fluids(event, type, { null }, { it.outputFluidHandler })
    }
}
