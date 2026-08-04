package rhx.lazy.integration.mysticalagriculture

import net.minecraft.core.Direction
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

internal object EssenceConverterCapabilities {
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            EssenceConverterRegistries.blockEntity.get(),
        ) { blockEntity, direction ->
            when (direction) {
                Direction.DOWN -> blockEntity.outputHandler
                null -> blockEntity.combinedHandler
                else -> blockEntity.inputHandler
            }
        }
    }
}
