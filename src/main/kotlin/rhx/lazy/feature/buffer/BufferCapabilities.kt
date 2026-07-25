package rhx.lazy.feature.buffer

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID

@EventBusSubscriber(modid = MOD_ID)
internal object BufferCapabilities {
    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            BufferRegistries.blockEntity.get(),
        ) { blockEntity, _ -> blockEntity.itemHandler }
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            BufferRegistries.blockEntity.get(),
        ) { blockEntity, _ -> blockEntity.fluidHandler }
    }
}
