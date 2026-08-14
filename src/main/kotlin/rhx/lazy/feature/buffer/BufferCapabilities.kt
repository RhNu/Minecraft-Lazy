package rhx.lazy.feature.buffer

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.IoFluidHandler
import rhx.lazy.core.io.IoItemHandler
import rhx.lazy.core.io.SideIoMode

@EventBusSubscriber(modid = MOD_ID)
internal object BufferCapabilities {
    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            BufferRegistries.blockEntity.get(),
        ) { blockEntity, side ->
            val sideMode = blockEntity.ioController.sideMode(side)
            if (sideMode == SideIoMode.NONE) {
                null
            } else {
                IoItemHandler(blockEntity.itemHandler, blockEntity.itemHandler) { blockEntity.ioController.sideMode(side) }
            }
        }
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            BufferRegistries.blockEntity.get(),
        ) { blockEntity, side ->
            val sideMode = blockEntity.ioController.sideMode(side)
            if (sideMode == SideIoMode.NONE) {
                null
            } else {
                IoFluidHandler(blockEntity.fluidHandler, blockEntity.fluidHandler) { blockEntity.ioController.sideMode(side) }
            }
        }
    }
}
