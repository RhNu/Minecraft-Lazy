package rhx.lazy.registry

import net.neoforged.bus.api.IEventBus

internal object ModRegistries {
    private val modules: List<RegistryModule> =
        listOf(
            ModBlocks,
            ModItems,
            ModBlockEntities,
            ModMenus,
            ModDataComponents,
            ModCreativeTabs,
        )

    fun register(bus: IEventBus) {
        modules.forEach { module -> module.register(bus) }
    }
}
