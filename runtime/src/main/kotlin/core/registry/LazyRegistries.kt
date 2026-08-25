package rhx.lazy.core.registry

import net.neoforged.bus.api.IEventBus
import rhx.lazy.core.configurator.ModularConfiguratorRegistries
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.machine.MachineCasingRegistries
import rhx.lazy.feature.machine.ProcessingCoreRegistries
import rhx.lazy.feature.protection.ProtectionRegistries
import rhx.lazy.feature.repairer.RepairerRegistries
import rhx.lazy.feature.replicator.ReplicatorRegistries
import rhx.lazy.feature.shaping.ShaperRegistries
import rhx.lazy.feature.simulation.SimulationRegistries
import rhx.lazy.feature.teleporter.TeleporterRegistries
import rhx.lazy.feature.voidworld.VoidWorldRegistries

internal object LazyRegistries {
    // Preserve the established order of entries that share a registry.
    private val modules: List<RegistryModule> =
        listOf(
            MachineCasingRegistries,
            ProcessingCoreRegistries,
            ConfigurationCardRegistries,
            ModularConfiguratorRegistries,
            BufferRegistries,
            TeleporterRegistries,
            EnergyRegistries,
            ReplicatorRegistries,
            RepairerRegistries,
            ShaperRegistries,
            SimulationRegistries,
            VoidWorldRegistries,
            LazyCreativeTabRegistry,
            ProtectionRegistries,
        )

    fun register(bus: IEventBus) {
        modules.forEach { module -> module.register(bus) }
    }
}
