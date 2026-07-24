package rhx.lazy.registry

import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.bus.api.IEventBus
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
        registry.registerBlockItem(
            "buffer",
            ModBlocks.buffer,
        ) { block, properties -> BufferBlockItem(block, properties) }

    val teleporter =
        registry.register(
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

    val energyBattery =
        registry.register(
            "energy_battery",
            Supplier {
                EnergyBatteryItem(
                    Item.Properties().apply {
                        stacksTo(1)
                        rarity(Rarity.RARE)
                        fireResistant()
                    },
                )
            },
        )

    val energySource =
        registry.registerBlockItem(
            "energy_source",
            ModBlocks.energySource,
        ) { block, properties -> EnergySourceBlockItem(block, properties) }

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
