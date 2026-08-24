package rhx.lazy.core.registry

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.configurator.ModularConfiguratorRegistries
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.itemcopier.ItemCopierRegistries
import rhx.lazy.feature.machine.MachineCasingRegistries
import rhx.lazy.feature.repairer.RepairerRegistries
import rhx.lazy.feature.shaping.ShaperRegistries
import rhx.lazy.feature.simulation.SimulationRegistries
import rhx.lazy.feature.teleporter.TeleporterRegistries
import java.util.function.Supplier

internal object LazyCreativeTabRegistry : RegistryModule {
    private val tabs: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)

    val tab =
        tabs.register(
            "lazy",
            Supplier {
                CreativeModeTab
                    .builder()
                    .title(Component.translatable("tab.lazy"))
                    .icon { ItemStack(BufferRegistries.item.get()) }
                    .displayItems { _, output ->
                        output.accept(MachineCasingRegistries.item.get())
                        output.accept(BufferRegistries.item.get())
                        output.accept(EnergyRegistries.sourceItem.get())
                        output.accept(ItemCopierRegistries.item.get())
                        output.accept(RepairerRegistries.item.get())
                        output.accept(ShaperRegistries.item.get())
                        output.accept(SimulationRegistries.item.get())
                        output.accept(ConfigurationCardRegistries.item.get())
                        output.accept(ModularConfiguratorRegistries.item.get())
                        output.accept(EnergyRegistries.batteryItem.get())
                        output.accept(TeleporterRegistries.item.get())
                        output.accept(SimulationRegistries.dataModelItem.get())
                        SimulationRegistries.allCoreItems().forEach { output.accept(it.get()) }
                    }.build()
            },
        )

    override fun register(bus: IEventBus) {
        tabs.register(bus)
    }
}
