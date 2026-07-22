package rhx.compose.registry

import net.neoforged.bus.api.IEventBus
import net.minecraft.world.item.Item
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.compose.MOD_ID
import rhx.compose.item.BufferBlockItem
import java.util.function.Supplier

internal object ModItems : RegistryModule {
    val registry: DeferredRegister.Items = DeferredRegister.createItems(MOD_ID)

    val buffer =
        registry.register(
            "buffer",
            Supplier { BufferBlockItem(ModBlocks.buffer.get(), Item.Properties()) },
        )

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
