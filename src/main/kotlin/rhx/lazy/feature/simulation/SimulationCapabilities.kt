package rhx.lazy.feature.simulation

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.IoFluidHandler
import rhx.lazy.core.io.IoItemHandler
import rhx.lazy.core.io.SideIoMode

@EventBusSubscriber(modid = MOD_ID)
internal object SimulationCapabilities {
    @SubscribeEvent
    fun register(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SimulationRegistries.blockEntity.get()) { blockEntity, side ->
            val sideMode = blockEntity.ioController.sideMode(side)
            if (sideMode == SideIoMode.NONE) {
                null
            } else {
                IoItemHandler(blockEntity.inputItemHandler, blockEntity.outputItemHandler) { blockEntity.ioController.sideMode(side) }
            }
        }
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SimulationRegistries.blockEntity.get()) { blockEntity, side ->
            val sideMode = blockEntity.ioController.sideMode(side)
            if (!sideMode.allowsOutput) {
                null
            } else {
                IoFluidHandler(null, blockEntity.outputFluidHandler) { blockEntity.ioController.sideMode(side) }
            }
        }
    }
}
