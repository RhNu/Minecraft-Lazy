package rhx.lazy.integration.ae2

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import java.util.function.Supplier

internal object Ae2Registries : RegistryModule {
    private val items = DeferredRegister.createItems(MOD_ID)

    val meOutputLinkCard =
        items.register(
            "me_output_link_card",
            Supplier { MeOutputLinkCard(Item.Properties().stacksTo(1)) },
        )

    override fun register(bus: IEventBus) {
        items.register(bus)
    }

    fun isLinkCard(stack: ItemStack): Boolean = stack.`is`(meOutputLinkCard.get())
}
