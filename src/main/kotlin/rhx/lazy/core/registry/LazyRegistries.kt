package rhx.lazy.core.registry

import net.neoforged.bus.api.IEventBus
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.protection.ProtectionRegistries
import rhx.lazy.feature.teleporter.TeleporterRegistries
import rhx.lazy.feature.voidworld.VoidWorldRegistries

internal object LazyRegistries {
    // Preserve the established order of entries that share a registry.
    private val modules: List<RegistryModule> =
        listOf(
            BufferRegistries,
            TeleporterRegistries,
            EnergyRegistries,
            VoidWorldRegistries,
            LazyCreativeTabRegistry,
            ProtectionRegistries,
        )

    fun register(bus: IEventBus) {
        modules.forEach { module -> module.register(bus) }
    }
}
