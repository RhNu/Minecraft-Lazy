package rhx.lazy.registry

import net.neoforged.bus.api.IEventBus

internal fun interface RegistryModule {
    fun register(bus: IEventBus)
}
