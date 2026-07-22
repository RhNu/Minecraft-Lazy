package rhx.lazy.registry

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import java.util.function.Supplier

internal object ModCreativeTabs : RegistryModule {
    val registry: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)

    val lazy =
        registry.register(
            "lazy",
            Supplier {
                CreativeModeTab
                    .builder()
                    .title(Component.translatable("tab.lazy"))
                    .icon { ItemStack(ModItems.buffer.get()) }
                    .displayItems { _, output -> output.accept(ModItems.buffer.get()) }
                    .build()
            },
        )

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
