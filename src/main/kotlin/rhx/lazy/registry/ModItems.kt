package rhx.lazy.registry

import net.neoforged.bus.api.IEventBus
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.item.BufferBlockItem
import rhx.lazy.item.EnergyBatteryItem
import rhx.lazy.item.EnergySourceBlockItem
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

    val energyBattery =
        registry.register(
            "energy_battery",
            Supplier {
                EnergyBatteryItem(
                    Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE)
                        .fireResistant(),
                )
            },
        )

    val energySource =
        registry.register(
            "energy_source",
            Supplier { EnergySourceBlockItem(ModBlocks.energySource.get(), Item.Properties()) },
        )

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
