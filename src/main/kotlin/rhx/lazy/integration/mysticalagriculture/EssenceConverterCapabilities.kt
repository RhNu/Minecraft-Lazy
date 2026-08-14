package rhx.lazy.integration.mysticalagriculture

import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.core.io.IoItemHandler
import rhx.lazy.core.io.SideIoMode

internal object EssenceConverterCapabilities {
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            EssenceConverterRegistries.blockEntity.get(),
        ) { blockEntity, direction ->
            val sideMode = blockEntity.ioController.sideMode(direction)
            if (sideMode == SideIoMode.NONE) {
                null
            } else {
                IoItemHandler(blockEntity.inputHandler, blockEntity.outputHandler) { blockEntity.ioController.sideMode(direction) }
            }
        }
    }
}
