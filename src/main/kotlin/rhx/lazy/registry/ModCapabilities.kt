package rhx.lazy.registry

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID
import rhx.lazy.util.InfiniteEnergyStorage

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
internal object ModCapabilities {
    private val energyBatteryStorage = InfiniteEnergyStorage()

    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            { _, _ -> energyBatteryStorage },
            ModItems.energyBattery.get(),
        )
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.energySource.get(),
        ) { blockEntity, _ -> blockEntity.energyStorage }
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.buffer.get(),
        ) { blockEntity, _ -> blockEntity.itemHandler }
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ModBlockEntities.buffer.get(),
        ) { blockEntity, _ -> blockEntity.fluidHandler }
    }
}
