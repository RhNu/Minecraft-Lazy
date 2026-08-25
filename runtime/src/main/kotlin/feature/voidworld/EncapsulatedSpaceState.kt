package rhx.lazy.feature.voidworld

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import rhx.lazy.LazyRuntime
import java.nio.charset.StandardCharsets
import java.util.UUID

internal class EncapsulatedSpaceState private constructor(
    private var nextOrdinal: Long,
    private var hubCreated: Boolean,
    spaces: Collection<EncapsulatedSpace>,
) : SavedData() {
    private val spacesById =
        LinkedHashMap<UUID, EncapsulatedSpace>().apply {
            spaces.forEach { space -> put(space.id, space) }
        }

    constructor() : this(0L, false, emptyList())

    fun allSpaces(): List<EncapsulatedSpace> = spacesById.values.toList()

    fun activeSpaces(): List<EncapsulatedSpace> = spacesById.values.filter { space -> space.status == EncapsulatedSpaceStatus.ACTIVE }

    fun find(id: UUID): EncapsulatedSpace? = spacesById[id]

    fun findContaining(pos: net.minecraft.core.BlockPos): EncapsulatedSpace? =
        spacesById.values.firstOrNull { space ->
            space.status == EncapsulatedSpaceStatus.ACTIVE && space.contains(pos)
        }

    fun countOwnedActive(ownerId: UUID): Int =
        spacesById.values.count { space ->
            space.ownerId == ownerId && space.status == EncapsulatedSpaceStatus.ACTIVE
        }

    fun reserve(ownerId: UUID): EncapsulatedSpace {
        val ordinal = nextOrdinal++
        val space =
            EncapsulatedSpace(
                id = spaceIdFor(ordinal),
                ownerId = ownerId,
                customName = null,
                allocationOrdinal = ordinal,
                anchorChunk = EncapsulatedSpaceAllocator.anchorFor(ordinal),
                status = EncapsulatedSpaceStatus.CREATING,
            )
        spacesById[space.id] = space
        setDirty()
        return space
    }

    fun put(space: EncapsulatedSpace) {
        spacesById[space.id] = space
        setDirty()
    }

    fun remove(id: UUID): EncapsulatedSpace? = spacesById.remove(id)?.also { setDirty() }

    fun isHubCreated(): Boolean = hubCreated

    fun markHubCreated() {
        if (hubCreated) return
        hubCreated = true
        setDirty()
    }

    override fun save(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ): CompoundTag {
        val snapshot = EncapsulatedSpaceSnapshot(nextOrdinal, hubCreated, spacesById.values.toList())
        val encoded =
            EncapsulatedSpaceSnapshot.CODEC
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), snapshot)
                .getOrThrow()
        require(encoded is CompoundTag) { "Encapsulated space codec did not produce a compound tag" }
        tag.merge(encoded)
        return tag
    }

    companion object {
        private const val DATA_NAME = "lazy_encapsulated_spaces"
        private val FACTORY = SavedData.Factory(::EncapsulatedSpaceState, ::load)

        fun get(server: MinecraftServer): EncapsulatedSpaceState = server.overworld().dataStorage.computeIfAbsent(FACTORY, DATA_NAME)

        internal fun spaceIdFor(ordinal: Long): UUID =
            UUID.nameUUIDFromBytes("lazy:encapsulated_space:$ordinal".toByteArray(StandardCharsets.UTF_8))

        private fun load(
            tag: CompoundTag,
            registries: HolderLookup.Provider,
        ): EncapsulatedSpaceState {
            val snapshot =
                EncapsulatedSpaceSnapshot.CODEC
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial { message -> LazyRuntime.logger.error("Failed to load encapsulated spaces: {}", message) }
                    .orElse(EncapsulatedSpaceSnapshot(0L, false, emptyList()))
            return EncapsulatedSpaceState(snapshot.nextOrdinal, snapshot.hubCreated, snapshot.spaces)
        }
    }
}

internal data class EncapsulatedSpaceSnapshot(
    val nextOrdinal: Long,
    val hubCreated: Boolean,
    val spaces: List<EncapsulatedSpace>,
) {
    companion object {
        val CODEC: Codec<EncapsulatedSpaceSnapshot> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        Codec.LONG.fieldOf("next_ordinal").forGetter(EncapsulatedSpaceSnapshot::nextOrdinal),
                        Codec.BOOL.fieldOf("hub_created").forGetter(EncapsulatedSpaceSnapshot::hubCreated),
                        EncapsulatedSpace.CODEC
                            .listOf()
                            .fieldOf("spaces")
                            .forGetter(EncapsulatedSpaceSnapshot::spaces),
                    ).apply(builder, ::EncapsulatedSpaceSnapshot)
            }
    }
}
