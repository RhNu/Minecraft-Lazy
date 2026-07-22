package rhx.compose.registry

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.compose.MOD_ID

internal object ModItems : RegistryModule {
    val registry: DeferredRegister.Items = DeferredRegister.createItems(MOD_ID)

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
