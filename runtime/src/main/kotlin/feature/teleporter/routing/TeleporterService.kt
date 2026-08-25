package rhx.lazy.feature.teleporter

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.portal.DimensionTransition
import net.minecraft.world.phys.Vec3
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.teleport.PlayerLandingResolver
import rhx.lazy.feature.voidworld.EncapsulatedSpace
import rhx.lazy.feature.voidworld.EncapsulatedSpaceService
import rhx.lazy.feature.voidworld.EncapsulatedSpaceState
import rhx.lazy.feature.voidworld.EncapsulatedSpaceStatus
import rhx.lazy.feature.voidworld.VoidHubPlatform
import rhx.lazy.feature.voidworld.VoidWorldKeys
import java.util.UUID

internal object TeleporterService {
    fun teleportToSpace(
        player: ServerPlayer,
        spaceId: UUID,
    ): TeleporterResult {
        val space =
            EncapsulatedSpaceState
                .get(player.server)
                .find(spaceId)
                ?.takeIf { candidate -> candidate.status == EncapsulatedSpaceStatus.ACTIVE }
                ?: return fail(player, SPACE_MISSING)
        if (!EncapsulatedSpaceService.canManage(player, space)) return fail(player, SPACE_NOT_OWNER)
        val level = player.server.getLevel(VoidWorldKeys.voidLevel) ?: return fail(player, DIMENSION_MISSING)
        if (!VoidHubPlatform.ensureCreated(player.server)) return fail(player, DIMENSION_MISSING)
        val destination = resolveSpaceLanding(level, space) ?: return fail(player, NO_SAFE_DESTINATION)
        return teleportIntoVoid(player, destination, selectedSpaceId = space.id)
    }

    fun teleportToHub(player: ServerPlayer): TeleporterResult {
        val level = player.server.getLevel(VoidWorldKeys.voidLevel) ?: return fail(player, DIMENSION_MISSING)
        if (!VoidHubPlatform.ensureCreated(player.server)) return fail(player, DIMENSION_MISSING)
        val position =
            PlayerLandingResolver.find(level, VoidHubPlatform.standingPos)
                ?: return fail(player, HUB_UNSAFE)
        return teleportIntoVoid(
            player,
            ResolvedDestination(
                SavedLocation(VoidWorldKeys.voidLevel, VoidHubPlatform.standingPos, player.yRot, player.xRot),
                position,
            ),
            selectedSpaceId = null,
        )
    }

    fun returnOutside(player: ServerPlayer): TeleporterResult {
        if (player.level().dimension() != VoidWorldKeys.voidLevel) return fail(player, NOT_IN_VOID)
        if (!validateActivation(player)) return TeleporterResult.Failure(ACTIVATION_REJECTED)

        val oldState = player.getData(TeleporterRegistries.playerState.get())
        val requested = oldState.externalReturn
        val destination =
            requested?.let { resolveOutside(player, it) } ?: resolveFallback(player)
                ?: return fail(player, NO_SAFE_DESTINATION)
        if (!move(player, destination)) return fail(player, TRANSITION_FAILED)

        player.setData(
            TeleporterRegistries.playerState.get(),
            oldState.copy(externalReturn = destination.location),
        )
        complete(player)
        return TeleporterResult.Success
    }

    fun rescueFromUnregisteredVoid(player: ServerPlayer) {
        if (player.level().dimension() != VoidWorldKeys.voidLevel) return
        val state = EncapsulatedSpaceState.get(player.server)
        if (VoidHubPlatform.isHubArea(player.blockPosition()) || state.findContaining(player.blockPosition()) != null) return

        val playerState = player.getData(TeleporterRegistries.playerState.get())
        val destination = playerState.externalReturn?.let { resolveOutside(player, it) } ?: resolveFallback(player) ?: return
        if (move(player, destination)) {
            player.setData(TeleporterRegistries.playerState.get(), playerState.copy(externalReturn = destination.location))
        }
    }

    private fun teleportIntoVoid(
        player: ServerPlayer,
        destination: ResolvedDestination,
        selectedSpaceId: UUID?,
    ): TeleporterResult {
        if (!validateActivation(player)) return TeleporterResult.Failure(ACTIVATION_REJECTED)
        val oldState = player.getData(TeleporterRegistries.playerState.get())
        val enteringFromOutside = player.level().dimension() != VoidWorldKeys.voidLevel
        val capturedReturn =
            if (enteringFromOutside) {
                val current = SavedLocation(player.level().dimension(), player.blockPosition(), player.yRot, player.xRot)
                TeleporterDestinationResolver(TeleporterConfigs.settings.safeSearchRadius.get())
                    .resolve(player.serverLevel(), current)
                    ?.location
                    ?: return fail(player, NO_SAFE_RETURN)
            } else {
                oldState.externalReturn
            }

        if (!move(player, destination)) return fail(player, TRANSITION_FAILED)
        player.setData(
            TeleporterRegistries.playerState.get(),
            TeleporterPlayerState(capturedReturn, selectedSpaceId ?: oldState.selectedSpaceId),
        )
        complete(player)
        return TeleporterResult.Success
    }

    private fun resolveOutside(
        player: ServerPlayer,
        requested: SavedLocation,
    ): ResolvedDestination? {
        if (requested.dimension == VoidWorldKeys.voidLevel) return null
        val level = player.server.getLevel(requested.dimension) ?: return null
        return TeleporterDestinationResolver(TeleporterConfigs.settings.safeSearchRadius.get()).resolve(level, requested)
    }

    private fun resolveFallback(player: ServerPlayer): ResolvedDestination? {
        val respawnPos = player.respawnPosition
        val respawnLevel = player.server.getLevel(player.respawnDimension)
        if (respawnPos != null && respawnLevel != null && respawnLevel.dimension() != VoidWorldKeys.voidLevel) {
            val requested = SavedLocation(respawnLevel.dimension(), respawnPos, player.respawnAngle, 0.0f)
            TeleporterDestinationResolver(TeleporterConfigs.settings.safeSearchRadius.get()).resolve(respawnLevel, requested)?.let {
                return it
            }
        }
        val overworld = player.server.overworld()
        val requested = SavedLocation(Level.OVERWORLD, overworld.sharedSpawnPos, player.yRot, player.xRot)
        return TeleporterDestinationResolver(TeleporterConfigs.settings.safeSearchRadius.get()).resolve(overworld, requested)
            ?: ResolvedDestination(requested, Vec3.atBottomCenterOf(requested.pos))
    }

    private fun resolveSpaceLanding(
        level: ServerLevel,
        space: EncapsulatedSpace,
    ): ResolvedDestination? {
        val candidates =
            buildList {
                for (x in space.innerMin.x..space.innerMax.x) {
                    for (y in space.innerMin.y until space.innerMax.y) {
                        for (z in space.innerMin.z..space.innerMax.z) {
                            val pos = BlockPos(x, y, z)
                            val dx = x - space.spawnPos.x
                            val dy = y - space.spawnPos.y
                            val dz = z - space.spawnPos.z
                            add(pos to (dx * dx + dz * dz to kotlin.math.abs(dy)))
                        }
                    }
                }
            }.sortedWith(
                compareBy<Pair<BlockPos, Pair<Int, Int>>> { it.second.first }
                    .thenBy { it.second.second }
                    .thenBy { it.first.y }
                    .thenBy { it.first.x }
                    .thenBy { it.first.z },
            )
        for ((pos) in candidates) {
            val landing = PlayerLandingResolver.find(level, pos) ?: continue
            return ResolvedDestination(
                SavedLocation(level.dimension(), pos, 0.0f, 0.0f),
                landing,
            )
        }
        return null
    }

    private fun move(
        player: ServerPlayer,
        destination: ResolvedDestination,
    ): Boolean {
        val targetLevel = player.server.getLevel(destination.location.dimension) ?: return false
        val targetChunk = ChunkPos(BlockPos.containing(destination.position))
        targetLevel.chunkSource.addRegionTicket(TicketType.POST_TELEPORT, targetChunk, 1, player.id)
        if (targetLevel === player.serverLevel()) {
            player.teleportTo(
                targetLevel,
                destination.position.x,
                destination.position.y,
                destination.position.z,
                destination.location.yaw,
                destination.location.pitch,
            )
        } else {
            val result =
                player.changeDimension(
                    DimensionTransition(
                        targetLevel,
                        destination.position,
                        Vec3.ZERO,
                        destination.location.yaw,
                        destination.location.pitch,
                        false,
                        DimensionTransition.PLAY_PORTAL_SOUND,
                    ),
                ) ?: return false
            result.deltaMovement = Vec3.ZERO
            result.fallDistance = 0.0f
        }
        player.deltaMovement = Vec3.ZERO
        player.fallDistance = 0.0f
        return true
    }

    private fun validateActivation(player: ServerPlayer): Boolean {
        if (TeleporterActivation.isDimensionBlacklisted(player)) {
            player.displayActionBar(TeleporterActivation.DIMENSION_BLACKLISTED)
            return false
        }
        if (TeleporterActivation.isOnCooldown(player)) {
            player.displayActionBar(COOLDOWN)
            return false
        }
        return true
    }

    private fun complete(player: ServerPlayer) {
        player.closeContainer()
        val cooldownSeconds = TeleporterConfigs.settings.cooldownSeconds.get()
        if (cooldownSeconds > 0) {
            player.cooldowns.addCooldown(TeleporterRegistries.item.get(), cooldownSeconds * TICKS_PER_SECOND)
        }
        player.displayActionBar(SUCCESS, cooldownSeconds)
    }

    private fun fail(
        player: ServerPlayer,
        key: String,
    ): TeleporterResult.Failure {
        player.displayActionBar(key)
        return TeleporterResult.Failure(key)
    }

    private const val TICKS_PER_SECOND = 20
    private const val ACTIVATION_REJECTED = "message.lazy.teleporter.activation_rejected"
    private const val COOLDOWN = "message.lazy.teleporter.cooldown"
    private const val SUCCESS = "message.lazy.teleporter.success"
    private const val SPACE_MISSING = "message.lazy.teleporter.space_not_found"
    private const val SPACE_NOT_OWNER = "message.lazy.teleporter.space_not_owner"
    private const val DIMENSION_MISSING = "message.lazy.teleporter.dimension_missing"
    private const val NO_SAFE_DESTINATION = "message.lazy.teleporter.no_safe_destination"
    private const val NO_SAFE_RETURN = "message.lazy.teleporter.no_safe_return"
    private const val TRANSITION_FAILED = "message.lazy.teleporter.transition_failed"
    private const val HUB_UNSAFE = "message.lazy.teleporter.hub_unsafe"
    private const val NOT_IN_VOID = "message.lazy.teleporter.not_in_void"
}

internal sealed interface TeleporterResult {
    data object Success : TeleporterResult

    data class Failure(
        val translationKey: String,
    ) : TeleporterResult
}
