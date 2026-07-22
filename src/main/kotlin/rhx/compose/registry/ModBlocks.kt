package rhx.compose.registry

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.compose.MOD_ID
import rhx.compose.block.BufferBlock
import java.util.function.Supplier

internal object ModBlocks : RegistryModule {
    val registry: DeferredRegister.Blocks = DeferredRegister.createBlocks(MOD_ID)

    val buffer = registry.register("buffer", Supplier(::BufferBlock))

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
