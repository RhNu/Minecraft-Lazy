package rhx.compose.registry

import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.compose.MOD_ID

internal object ModMenus : RegistryModule {
    val registry: DeferredRegister<MenuType<*>> = DeferredRegister.create(Registries.MENU, MOD_ID)

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
