package rhx.lazy.feature.machine

import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.core.registry.registerBlockItem
import java.util.function.Supplier

internal object MachineCasingRegistries : RegistryModule {
    private val blocks = DeferredRegister.createBlocks(MOD_ID)
    private val items = DeferredRegister.createItems(MOD_ID)

    val block =
        blocks.register(
            "machine_casing",
            Supplier {
                Block(
                    BlockBehaviour.Properties
                        .of()
                        .strength(0.5f)
                        .sound(SoundType.METAL),
                )
            },
        )
    val item =
        items.registerBlockItem(
            "machine_casing",
            block,
        ) { registeredBlock, properties -> BlockItem(registeredBlock, properties) }

    override fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
    }
}
