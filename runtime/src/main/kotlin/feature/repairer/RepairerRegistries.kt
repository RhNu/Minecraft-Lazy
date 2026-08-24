package rhx.lazy.feature.repairer

import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.core.registry.buildType
import rhx.lazy.core.registry.registerBlockItem
import java.util.function.Supplier

internal object RepairerRegistries : RegistryModule {
    private val blocks = DeferredRegister.createBlocks(MOD_ID)
    private val items = DeferredRegister.createItems(MOD_ID)
    private val blockEntities: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)

    val block = blocks.register("repairer", Supplier(::RepairerBlock))
    val item =
        items.registerBlockItem(
            "repairer",
            block,
        ) { registeredBlock, properties -> BlockItem(registeredBlock, properties) }
    val blockEntity =
        blockEntities.register(
            "repairer",
            Supplier {
                BlockEntityType.Builder
                    .of(::RepairerBlockEntity, block.get())
                    .buildType()
            },
        )

    override fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
        blockEntities.register(bus)
    }
}
