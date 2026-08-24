package rhx.lazy.core.io

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.integration.api.LazyInternalApi
import java.util.function.Supplier

@LazyInternalApi
public object ConfigurationCardRegistries : RegistryModule {
    private val items = DeferredRegister.createItems(MOD_ID)

    val item =
        items.register(
            "configuration_card",
            Supplier { ConfigurationCardItem(Item.Properties().stacksTo(1)) },
        )

    override fun register(bus: IEventBus) {
        items.register(bus)
    }

    fun isCard(stack: ItemStack): Boolean = stack.`is`(item.get())
}
