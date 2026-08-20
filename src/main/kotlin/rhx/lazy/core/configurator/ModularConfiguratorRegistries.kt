package rhx.lazy.core.configurator

import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import java.util.function.Supplier

internal object ModularConfiguratorRegistries : RegistryModule {
    private val items = DeferredRegister.createItems(MOD_ID)
    private val dataComponents: DeferredRegister.DataComponents =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)

    val item =
        items.register(
            "modular_configurator",
            Supplier { ModularConfiguratorItem(Item.Properties().stacksTo(1)) },
        )

    val dataComponent =
        dataComponents.registerComponentType("modular_configurator_data") { builder ->
            builder.persistent(ModularConfiguratorData.CODEC)
        }

    override fun register(bus: IEventBus) {
        dataComponents.register(bus)
        items.register(bus)
    }

    fun isConfigurator(stack: ItemStack): Boolean = stack.`is`(item.get())
}
