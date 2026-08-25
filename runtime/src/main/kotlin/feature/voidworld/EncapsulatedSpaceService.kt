package rhx.lazy.feature.voidworld

import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TicketType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.LazyRuntime
import rhx.lazy.core.teleport.PlayerLandingResolver
import rhx.lazy.feature.teleporter.TeleporterConfigs
import java.util.UUID

internal sealed interface EncapsulatedSpaceResult {
    data class Success(
        val space: EncapsulatedSpace? = null,
    ) : EncapsulatedSpaceResult

    data class Failure(
        val translationKey: String,
    ) : EncapsulatedSpaceResult
}

internal enum class EncapsulatedShellPart {
    AIR,
    PANEL,
    FRAME,
}

internal object EncapsulatedSpaceService {
    fun listFor(player: ServerPlayer): List<EncapsulatedSpace> =
        EncapsulatedSpaceState
            .get(player.server)
            .activeSpaces()
            .asSequence()
            .filter { space -> canManage(player, space) }
            .sortedByDescending(EncapsulatedSpace::allocationOrdinal)
            .toList()

    fun create(player: ServerPlayer): EncapsulatedSpaceResult {
        val state = EncapsulatedSpaceState.get(player.server)
        if (!canCreateSpace(
                state.countOwnedActive(player.uuid),
                TeleporterConfigs.settings.maxSpacesPerPlayer.get(),
                isOperator(player),
            )
        ) {
            return EncapsulatedSpaceResult.Failure(SPACE_LIMIT)
        }
        val level =
            player.server.getLevel(VoidWorldKeys.voidLevel)
                ?: return EncapsulatedSpaceResult.Failure(DIMENSION_MISSING)

        repeat(MAX_ALLOCATION_ATTEMPTS) {
            val space = state.reserve(player.uuid)
            if (!isWithinWorldBorder(level, space) || overlapsRegisteredSpace(state, space)) {
                state.remove(space.id)
                return@repeat
            }

            return try {
                if (withLoadedChunks(level, space) { containsExistingShell(level, space) }) {
                    state.remove(space.id)
                    return@repeat
                }
                withLoadedChunks(level, space) {
                    generate(level, space)
                }
                val active = space.copy(status = EncapsulatedSpaceStatus.ACTIVE)
                state.put(active)
                EncapsulatedSpaceChunkLoading.force(level, active)
                EncapsulatedSpaceResult.Success(active)
            } catch (exception: RuntimeException) {
                LazyRuntime.logger.error("Failed to create encapsulated space {}", space.id, exception)
                runCatching { EncapsulatedSpaceChunkLoading.unforce(level, space) }
                runCatching { withLoadedChunks(level, space) { clear(level, space) } }
                state.remove(space.id)
                EncapsulatedSpaceResult.Failure(CREATE_FAILED)
            }
        }
        return EncapsulatedSpaceResult.Failure(NO_ALLOCATION)
    }

    fun rename(
        player: ServerPlayer,
        id: UUID,
        requestedName: String,
    ): EncapsulatedSpaceResult {
        val state = EncapsulatedSpaceState.get(player.server)
        val space =
            state.find(id)?.takeIf { it.status == EncapsulatedSpaceStatus.ACTIVE }
                ?: return EncapsulatedSpaceResult.Failure(NOT_FOUND)
        if (!canManage(player, space)) return EncapsulatedSpaceResult.Failure(NOT_OWNER)
        val name = normalizeName(requestedName) ?: return EncapsulatedSpaceResult.Failure(INVALID_NAME)
        val renamed = space.copy(customName = name)
        state.put(renamed)
        return EncapsulatedSpaceResult.Success(renamed)
    }

    fun delete(
        player: ServerPlayer,
        id: UUID,
    ): EncapsulatedSpaceResult {
        val state = EncapsulatedSpaceState.get(player.server)
        val space =
            state.find(id)?.takeIf { it.status == EncapsulatedSpaceStatus.ACTIVE }
                ?: return EncapsulatedSpaceResult.Failure(NOT_FOUND)
        if (!canManage(player, space)) return EncapsulatedSpaceResult.Failure(NOT_OWNER)
        val level =
            player.server.getLevel(VoidWorldKeys.voidLevel)
                ?: return EncapsulatedSpaceResult.Failure(DIMENSION_MISSING)

        val occupants = level.players().filter { occupant -> space.entityBounds.intersects(occupant.boundingBox) }
        val hubLanding = PlayerLandingResolver.find(level, VoidHubPlatform.standingPos)
        if (occupants.isNotEmpty() && hubLanding == null) {
            return EncapsulatedSpaceResult.Failure(HUB_UNSAFE)
        }

        state.put(space.copy(status = EncapsulatedSpaceStatus.DELETING))
        return try {
            if (hubLanding != null) {
                occupants.forEach { occupant ->
                    occupant.teleportTo(
                        level,
                        hubLanding.x,
                        hubLanding.y,
                        hubLanding.z,
                        occupant.yRot,
                        occupant.xRot,
                    )
                }
            }
            withLoadedChunks(level, space) {
                level
                    .getEntities(null, space.entityBounds) { entity -> entity !is Player }
                    .forEach { entity -> entity.discard() }
                clear(level, space)
            }
            EncapsulatedSpaceChunkLoading.unforce(level, space)
            state.remove(space.id)
            EncapsulatedSpaceResult.Success()
        } catch (exception: RuntimeException) {
            LazyRuntime.logger.error("Failed to delete encapsulated space {}", space.id, exception)
            state.put(space.copy(status = EncapsulatedSpaceStatus.ACTIVE))
            EncapsulatedSpaceResult.Failure(DELETE_FAILED)
        }
    }

    fun recover(server: MinecraftServer) {
        val level = server.getLevel(VoidWorldKeys.voidLevel) ?: return
        val state = EncapsulatedSpaceState.get(server)
        state.allSpaces().forEach { space ->
            when (space.status) {
                EncapsulatedSpaceStatus.ACTIVE -> EncapsulatedSpaceChunkLoading.force(level, space)
                EncapsulatedSpaceStatus.CREATING -> {
                    runCatching { withLoadedChunks(level, space) { generate(level, space) } }
                        .onSuccess {
                            val active = space.copy(status = EncapsulatedSpaceStatus.ACTIVE)
                            state.put(active)
                            EncapsulatedSpaceChunkLoading.force(level, active)
                        }.onFailure { error ->
                            LazyRuntime.logger.error("Failed to recover space creation {}", space.id, error)
                            runCatching { withLoadedChunks(level, space) { clear(level, space) } }
                            state.remove(space.id)
                        }
                }
                EncapsulatedSpaceStatus.DELETING -> {
                    runCatching {
                        withLoadedChunks(level, space) {
                            level
                                .getEntities(null, space.entityBounds) { entity -> entity !is Player }
                                .forEach { entity -> entity.discard() }
                            clear(level, space)
                        }
                    }.onSuccess {
                        EncapsulatedSpaceChunkLoading.unforce(level, space)
                        state.remove(space.id)
                    }.onFailure { error -> LazyRuntime.logger.error("Failed to resume space deletion {}", space.id, error) }
                }
            }
        }
    }

    fun canManage(
        player: ServerPlayer,
        space: EncapsulatedSpace,
    ): Boolean = canManage(space.ownerId, player.uuid, isOperator(player))

    internal fun canManage(
        ownerId: UUID,
        actorId: UUID,
        operator: Boolean,
    ): Boolean = ownerId == actorId || operator

    internal fun canCreateSpace(
        activeCount: Int,
        limit: Int,
        operator: Boolean,
    ): Boolean = operator || activeCount < limit

    internal fun normalizeName(value: String): String? {
        val normalized = value.trim()
        val codePointCount = normalized.codePointCount(0, normalized.length)
        if (codePointCount !in 1..MAX_NAME_CODE_POINTS) return null
        if (normalized.codePoints().anyMatch(Character::isISOControl)) return null
        return normalized
    }

    internal fun shellStateAt(
        space: EncapsulatedSpace,
        pos: BlockPos,
    ): BlockState =
        when (shellPartAt(space, pos)) {
            EncapsulatedShellPart.AIR -> Blocks.AIR.defaultBlockState()
            EncapsulatedShellPart.PANEL -> VoidWorldRegistries.spaceWall.get().defaultBlockState()
            EncapsulatedShellPart.FRAME -> VoidWorldRegistries.spaceFrame.get().defaultBlockState()
        }

    internal fun shellPartAt(
        space: EncapsulatedSpace,
        pos: BlockPos,
    ): EncapsulatedShellPart {
        val min = space.outerMin
        val max = space.outerMax
        val boundaryPlanes =
            listOf(
                pos.x == min.x || pos.x == max.x,
                pos.y == min.y || pos.y == max.y,
                pos.z == min.z || pos.z == max.z,
            ).count { it }
        return when {
            boundaryPlanes >= 2 -> EncapsulatedShellPart.FRAME
            boundaryPlanes == 1 -> EncapsulatedShellPart.PANEL
            else -> EncapsulatedShellPart.AIR
        }
    }

    private fun generate(
        level: ServerLevel,
        space: EncapsulatedSpace,
    ) {
        forEachBlock(space) { pos ->
            level.setBlock(pos, shellStateAt(space, pos), Block.UPDATE_ALL)
        }
    }

    private fun clear(
        level: ServerLevel,
        space: EncapsulatedSpace,
    ) {
        val air = Blocks.AIR.defaultBlockState()
        forEachBlock(space) { pos -> level.setBlock(pos, air, Block.UPDATE_ALL) }
    }

    private fun forEachBlock(
        space: EncapsulatedSpace,
        action: (BlockPos) -> Unit,
    ) {
        val min = space.outerMin
        val max = space.outerMax
        val mutable = BlockPos.MutableBlockPos()
        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                for (z in min.z..max.z) {
                    action(mutable.set(x, y, z))
                }
            }
        }
    }

    private fun isWithinWorldBorder(
        level: ServerLevel,
        space: EncapsulatedSpace,
    ): Boolean =
        level.worldBorder.isWithinBounds(space.outerMin) &&
            level.worldBorder.isWithinBounds(space.outerMax)

    private fun overlapsRegisteredSpace(
        state: EncapsulatedSpaceState,
        candidate: EncapsulatedSpace,
    ): Boolean =
        state
            .allSpaces()
            .asSequence()
            .filter { space -> space.id != candidate.id }
            .any { space -> space.entityBounds.intersects(candidate.entityBounds) }

    private fun containsExistingShell(
        level: ServerLevel,
        space: EncapsulatedSpace,
    ): Boolean {
        val wall = VoidWorldRegistries.spaceWall.get()
        val frame = VoidWorldRegistries.spaceFrame.get()
        var found = false
        forEachBlock(space) { pos ->
            if (!found) {
                val block = level.getBlockState(pos).block
                found = block === wall || block === frame
            }
        }
        return found
    }

    private inline fun <T> withLoadedChunks(
        level: ServerLevel,
        space: EncapsulatedSpace,
        action: () -> T,
    ): T {
        val ticketKey = space.outerMin
        val chunks =
            ChunkPos
                .rangeClosed(ChunkPos(space.outerMin), ChunkPos(space.outerMax))
                .toList()
        chunks.forEach { chunk ->
            level.chunkSource.addRegionTicket(TicketType.PORTAL, chunk, REGION_TICKET_LEVEL, ticketKey)
            level.getChunk(chunk.x, chunk.z)
        }
        return try {
            action()
        } finally {
            chunks.forEach { chunk ->
                level.chunkSource.removeRegionTicket(TicketType.PORTAL, chunk, REGION_TICKET_LEVEL, ticketKey)
            }
        }
    }

    private fun isOperator(player: ServerPlayer): Boolean = player.hasPermissions(OP_PERMISSION_LEVEL)

    private const val OP_PERMISSION_LEVEL = 2
    private const val MAX_NAME_CODE_POINTS = 32
    private const val MAX_ALLOCATION_ATTEMPTS = 4_096
    private const val REGION_TICKET_LEVEL = 1

    const val SPACE_LIMIT = "message.lazy.teleporter.space_limit"
    const val DIMENSION_MISSING = "message.lazy.teleporter.dimension_missing"
    const val CREATE_FAILED = "message.lazy.teleporter.space_create_failed"
    const val DELETE_FAILED = "message.lazy.teleporter.space_delete_failed"
    const val NO_ALLOCATION = "message.lazy.teleporter.no_space_allocation"
    const val NOT_FOUND = "message.lazy.teleporter.space_not_found"
    const val NOT_OWNER = "message.lazy.teleporter.space_not_owner"
    const val INVALID_NAME = "message.lazy.teleporter.space_invalid_name"
    const val HUB_UNSAFE = "message.lazy.teleporter.hub_unsafe"
}
