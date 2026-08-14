package rhx.lazy.feature.energy

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID

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
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            EnergyRegistries.sourceBlockEntity.get(),
        ) { blockEntity, side ->
            blockEntity.energyStorage.takeIf { blockEntity.ioController.sideMode(side).allowsOutput }
        }
    }
}
