package rhx.lazy.feature.energy

import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.core.registry.buildType
import rhx.lazy.core.registry.registerBlockItem
import java.util.function.Supplier

internal object EnergyRegistries : RegistryModule {
    private val blocks = DeferredRegister.createBlocks(MOD_ID)
    private val items = DeferredRegister.createItems(MOD_ID)
    private val blockEntities: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)

    val sourceBlock = blocks.register("energy_source", Supplier(::EnergySourceBlock))
    val batteryItem =
        items.register(
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
    val sourceItem =
        items.registerBlockItem(
            "energy_source",
            sourceBlock,
        ) { registeredBlock, properties -> EnergySourceBlockItem(registeredBlock, properties) }
    val sourceBlockEntity =
        blockEntities.register(
            "energy_source",
            Supplier {
                BlockEntityType.Builder
                    .of(::EnergySourceBlockEntity, sourceBlock.get())
                    .buildType()
            },
        )

    override fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
        blockEntities.register(bus)
    }
}
