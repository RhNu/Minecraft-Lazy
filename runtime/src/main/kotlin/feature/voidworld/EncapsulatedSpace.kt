package rhx.lazy.feature.voidworld

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.Optional
import java.util.UUID

internal enum class EncapsulatedSpaceStatus {
    CREATING,
    ACTIVE,
    DELETING,
    ;

    companion object {
        val CODEC: Codec<EncapsulatedSpaceStatus> =
            Codec.STRING.xmap(
                { value -> entries.first { status -> status.name.equals(value, ignoreCase = true) } },
                { status -> status.name.lowercase() },
            )
    }
}

internal data class EncapsulatedSpace(
    val id: UUID,
    val ownerId: UUID,
    val customName: String?,
    val allocationOrdinal: Long,
    val anchorChunk: ChunkPos,
    val status: EncapsulatedSpaceStatus,
) {
    val shortId: String
        get() = id.toString().replace("-", "").take(SHORT_ID_LENGTH)

    val outerMin: BlockPos
        get() = BlockPos(anchorChunk.minBlockX, FLOOR_Y, anchorChunk.minBlockZ)

    val outerMax: BlockPos
        get() = BlockPos(anchorChunk.minBlockX + OUTER_SIZE - 1, CEILING_Y, anchorChunk.minBlockZ + OUTER_SIZE - 1)

    val innerMin: BlockPos
        get() = BlockPos(anchorChunk.minBlockX + 1, FLOOR_Y + 1, anchorChunk.minBlockZ + 1)

    val innerMax: BlockPos
        get() = BlockPos(anchorChunk.minBlockX + INNER_SIZE, CEILING_Y - 1, anchorChunk.minBlockZ + INNER_SIZE)

    val spawnPos: BlockPos
        get() = BlockPos(anchorChunk.minBlockX + 8, FLOOR_Y + 1, anchorChunk.minBlockZ + 8)

    val entityBounds: AABB
        get() = AABB(Vec3.atLowerCornerOf(outerMin), Vec3.atLowerCornerOf(outerMax.offset(1, 1, 1)))

    fun displayName(): Component =
        customName?.let(Component::literal)
            ?: Component.translatable(DEFAULT_NAME_KEY, shortId)

    fun contains(pos: BlockPos): Boolean =
        pos.x in outerMin.x..outerMax.x &&
            pos.y in outerMin.y..outerMax.y &&
            pos.z in outerMin.z..outerMax.z

    companion object {
        const val FLOOR_Y = 64
        const val CEILING_Y = 80
        const val INNER_SIZE = 15
        const val OUTER_SIZE = 17
        const val SHORT_ID_LENGTH = 8
        const val DEFAULT_NAME_KEY = "space.lazy.default_name"

        val CODEC: Codec<EncapsulatedSpace> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        UUIDUtil.CODEC.fieldOf("id").forGetter(EncapsulatedSpace::id),
                        UUIDUtil.CODEC.fieldOf("owner").forGetter(EncapsulatedSpace::ownerId),
                        Codec.STRING
                            .optionalFieldOf("name")
                            .forGetter { space -> Optional.ofNullable(space.customName) },
                        Codec.LONG.fieldOf("ordinal").forGetter(EncapsulatedSpace::allocationOrdinal),
                        Codec.INT.fieldOf("chunk_x").forGetter { space -> space.anchorChunk.x },
                        Codec.INT.fieldOf("chunk_z").forGetter { space -> space.anchorChunk.z },
                        EncapsulatedSpaceStatus.CODEC.fieldOf("status").forGetter(EncapsulatedSpace::status),
                    ).apply(builder) { id, owner, name, ordinal, chunkX, chunkZ, status ->
                        EncapsulatedSpace(
                            id = id,
                            ownerId = owner,
                            customName = name.orElse(null),
                            allocationOrdinal = ordinal,
                            anchorChunk = ChunkPos(chunkX, chunkZ),
                            status = status,
                        )
                    }
            }
    }
}
