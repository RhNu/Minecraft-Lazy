package rhx.lazy.registry

import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.block.entity.BufferBlockEntity
import java.util.function.Supplier

internal object ModBlockEntities : RegistryModule {
    val registry: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)

    val buffer =
        registry.register(
            "buffer",
            Supplier {
                BlockEntityType.Builder
                    .of(::BufferBlockEntity, ModBlocks.buffer.get())
                    .buildType()
            },
        )

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
