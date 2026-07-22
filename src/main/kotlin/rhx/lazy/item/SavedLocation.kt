package rhx.lazy.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

internal data class SavedLocation(
    val dimension: ResourceKey<Level>,
    val pos: BlockPos,
    val yaw: Float,
    val pitch: Float,
) {
    companion object {
        val CODEC: Codec<SavedLocation> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        ResourceKey
                            .codec(Registries.DIMENSION)
                            .fieldOf("dimension")
                            .forGetter(SavedLocation::dimension),
                        BlockPos.CODEC.fieldOf("pos").forGetter(SavedLocation::pos),
                        Codec.FLOAT.fieldOf("yaw").forGetter(SavedLocation::yaw),
                        Codec.FLOAT.fieldOf("pitch").forGetter(SavedLocation::pitch),
                    ).apply(builder, ::SavedLocation)
            }
    }
}
