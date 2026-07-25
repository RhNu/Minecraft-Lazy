package rhx.lazy.core.registry

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
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
                        output.accept(BufferRegistries.item.get())
                        output.accept(TeleporterRegistries.item.get())
                        output.accept(EnergyRegistries.batteryItem.get())
                        output.accept(EnergyRegistries.sourceItem.get())
                    }.build()
            },
        )

    override fun register(bus: IEventBus) {
        tabs.register(bus)
    }
}
