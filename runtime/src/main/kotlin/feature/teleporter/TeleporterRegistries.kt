package rhx.lazy.feature.teleporter

import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.integration.api.LazyInternalApi
import java.util.function.Supplier

@LazyInternalApi
public object TeleporterRegistries : RegistryModule {
    private val items = DeferredRegister.createItems(MOD_ID)
    private val dataComponents: DeferredRegister.DataComponents =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)

    val item =
        items.register(
            "teleporter",
            Supplier {
                TeleporterItem(
                    Item.Properties().apply {
                        stacksTo(1)
                        rarity(Rarity.EPIC)
                        fireResistant()
                    },
                )
            },
        )
    internal val dataComponent =
        dataComponents.registerComponentType("teleporter_data") { builder ->
            builder.persistent(TeleporterData.CODEC)
        }

    override fun register(bus: IEventBus) {
        items.register(bus)
        dataComponents.register(bus)
    }
}
