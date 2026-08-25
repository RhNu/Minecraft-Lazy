package rhx.lazy.feature.replicator

import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.core.registry.buildType
import rhx.lazy.core.registry.registerBlockItem
import java.util.function.Supplier

internal object ReplicatorRegistries : RegistryModule {
    private val blocks = DeferredRegister.createBlocks(MOD_ID)
    private val items = DeferredRegister.createItems(MOD_ID)
    private val blockEntities: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)

    val block = blocks.register("replicator", Supplier(::ReplicatorBlock))
    val item =
        items.registerBlockItem(
            "replicator",
            block,
        ) { registeredBlock, properties -> ReplicatorBlockItem(registeredBlock, properties) }
    val blockEntity =
        blockEntities.register(
            "replicator",
            Supplier {
                BlockEntityType.Builder
                    .of({ pos, state -> ReplicatorBlockEntity(pos, state) }, block.get())
                    .buildType()
            },
        )

    override fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
        blockEntities.register(bus)
    }
}
