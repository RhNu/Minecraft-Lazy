package rhx.lazy.core.registry

import net.neoforged.bus.api.IEventBus
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public fun interface RegistryModule {
    fun register(bus: IEventBus)
}
