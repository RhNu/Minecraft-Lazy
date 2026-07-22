package rhx.compose.registry

import net.neoforged.bus.api.IEventBus

internal fun interface RegistryModule {
    fun register(bus: IEventBus)
}
