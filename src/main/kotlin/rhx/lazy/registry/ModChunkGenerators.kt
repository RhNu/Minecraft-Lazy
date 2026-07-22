package rhx.lazy.registry

import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.world.GridChunkGenerator
import java.util.function.Supplier

internal object ModChunkGenerators : RegistryModule {
    val registry = DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID)

    val gridGenerator =
        registry.register(
            "grid_generator",
            Supplier { GridChunkGenerator.MAP_CODEC },
        )

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
