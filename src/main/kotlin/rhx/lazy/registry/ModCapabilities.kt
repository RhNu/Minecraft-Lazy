package rhx.lazy.registry

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
internal object ModCapabilities {
    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
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
