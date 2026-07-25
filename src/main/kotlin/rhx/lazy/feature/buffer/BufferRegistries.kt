package rhx.lazy.feature.buffer

import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.core.registry.buildType
import rhx.lazy.core.registry.registerBlockItem
import java.util.function.Supplier

internal object BufferRegistries : RegistryModule {
    private val blocks = DeferredRegister.createBlocks(MOD_ID)
    private val items = DeferredRegister.createItems(MOD_ID)
    private val blockEntities: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)

    val block = blocks.register("buffer", Supplier(::BufferBlock))
    val item =
        items.registerBlockItem(
            "buffer",
            block,
        ) { registeredBlock, properties -> BufferBlockItem(registeredBlock, properties) }
    val blockEntity =
        blockEntities.register(
            "buffer",
            Supplier {
                BlockEntityType.Builder
                    .of(::BufferBlockEntity, block.get())
                    .buildType()
            },
        )

    override fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
        blockEntities.register(bus)
    }
}
