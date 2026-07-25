package rhx.lazy.feature.voidworld

import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import java.util.function.Supplier

internal object VoidWorldRegistries : RegistryModule {
    private val chunkGenerators = DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID)

    val gridGenerator =
        chunkGenerators.register(
            "grid_generator",
            Supplier { GridChunkGenerator.MAP_CODEC },
        )

    override fun register(bus: IEventBus) {
        chunkGenerators.register(bus)
    }
}
