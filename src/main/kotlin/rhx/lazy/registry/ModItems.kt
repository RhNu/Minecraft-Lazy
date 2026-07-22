package rhx.lazy.registry

import net.neoforged.bus.api.IEventBus
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.item.BufferBlockItem
import rhx.lazy.item.TeleporterItem
import java.util.function.Supplier

internal object ModItems : RegistryModule {
    val registry: DeferredRegister.Items = DeferredRegister.createItems(MOD_ID)

    val buffer =
        registry.register(
            "buffer",
            Supplier { BufferBlockItem(ModBlocks.buffer.get(), Item.Properties()) },
        )

    val teleporter =
        registry.register(
            "teleporter",
            Supplier {
                TeleporterItem(
                    Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.EPIC)
                        .fireResistant(),
                )
            },
        )

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
