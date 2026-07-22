package rhx.compose.registry

import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.compose.MOD_ID

internal object ModBlockEntities : RegistryModule {
    val registry: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
