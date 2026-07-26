package rhx.lazy.integration.botanypots

import net.minecraft.core.Direction
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

internal object PlanterCapabilities {
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            PlanterRegistries.blockEntity.get(),
        ) { blockEntity, side ->
            blockEntity.bottomOutputHandler.takeIf { side == Direction.DOWN }
        }
    }
}
