package rhx.lazy.feature.simulation

import net.minecraft.core.Direction
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import rhx.lazy.MOD_ID

@EventBusSubscriber(modid = MOD_ID)
internal object SimulationCapabilities {
    @SubscribeEvent
    fun register(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SimulationRegistries.blockEntity.get()) { blockEntity, side ->
            if (side == Direction.DOWN) blockEntity.outputItemHandler else blockEntity.inputItemHandler
        }
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SimulationRegistries.blockEntity.get()) { blockEntity, _ ->
            blockEntity.outputFluidHandler
        }
    }
}
