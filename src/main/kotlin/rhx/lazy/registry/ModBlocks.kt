package rhx.lazy.registry

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.block.BufferBlock
import rhx.lazy.block.EnergySourceBlock
import java.util.function.Supplier

internal object ModBlocks : RegistryModule {
    val registry: DeferredRegister.Blocks = DeferredRegister.createBlocks(MOD_ID)

    val buffer = registry.register("buffer", Supplier(::BufferBlock))
    val energySource = registry.register("energy_source", Supplier(::EnergySourceBlock))

    override fun register(bus: IEventBus) {
        registry.register(bus)
    }
}
