package rhx.lazy.registry

import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import rhx.lazy.MOD_ID
import rhx.lazy.menu.BufferMenu
import java.util.function.Supplier

internal object ModMenus : RegistryModule {
    val registry: DeferredRegister<MenuType<*>> = DeferredRegister.create(Registries.MENU, MOD_ID)

    val buffer =
        registry.register(
            "buffer",
            Supplier { IMenuTypeExtension.create(::BufferMenu) },
        )

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
