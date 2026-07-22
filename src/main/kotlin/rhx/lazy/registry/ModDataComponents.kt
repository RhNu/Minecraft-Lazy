package rhx.lazy.registry

import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID

internal object ModDataComponents : RegistryModule {
    val registry: DeferredRegister.DataComponents =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
