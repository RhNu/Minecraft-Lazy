package rhx.lazy.feature.voidworld

import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import java.util.function.Supplier

internal object VoidWorldRegistries : RegistryModule {
    private val chunkGenerators = DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID)
    private val blocks = DeferredRegister.createBlocks(MOD_ID)

    val voidGenerator =
        chunkGenerators.register(
            "void_generator",
            Supplier { VoidChunkGenerator.MAP_CODEC },
        )

    val spaceWall =
        blocks.register(
            "encapsulated_space_wall",
            Supplier {
                ProtectedSpaceWallBlock(
                    BlockBehaviour.Properties
                        .ofFullCopy(Blocks.GLASS)
                        .strength(-1.0f, 3_600_000.0f)
                        .lightLevel { 15 }
                        .pushReaction(PushReaction.BLOCK),
                )
            },
        )

    val spaceFrame =
        blocks.register(
            "encapsulated_space_frame",
            Supplier {
                ProtectedSpaceBlock(
                    BlockBehaviour.Properties
                        .of()
                        .strength(-1.0f, 3_600_000.0f)
                        .sound(SoundType.METAL)
                        .pushReaction(PushReaction.BLOCK),
                )
            },
        )

    override fun register(bus: IEventBus) {
        chunkGenerators.register(bus)
        blocks.register(bus)
    }
}
