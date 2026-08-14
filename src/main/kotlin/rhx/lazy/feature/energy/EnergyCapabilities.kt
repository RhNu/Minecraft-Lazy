package rhx.lazy.feature.energy

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.IoCapabilityRegistration

@EventBusSubscriber(modid = MOD_ID)
internal object EnergyCapabilities {
    private val energyBatteryStorage = InfiniteEnergyStorage()

    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            { _, _ -> energyBatteryStorage },
            EnergyRegistries.batteryItem.get(),
        )
        IoCapabilityRegistration.energyOutput(event, EnergyRegistries.sourceBlockEntity.get()) { it.energyStorage }
    }
}
