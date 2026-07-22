package rhx.compose.registry

import net.minecraft.core.registries.Registries
import net.minecraft.world.item.CreativeModeTab
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.compose.MOD_ID

internal object ModCreativeTabs : RegistryModule {
    val registry: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
