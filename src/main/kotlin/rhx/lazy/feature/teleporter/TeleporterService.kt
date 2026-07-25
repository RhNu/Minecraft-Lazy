package rhx.lazy.feature.teleporter

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.portal.DimensionTransition
import net.minecraft.world.phys.Vec3
import rhx.lazy.feature.voidworld.GridChunkGenerator
import rhx.lazy.feature.voidworld.VoidWorldKeys

internal data class TeleporterSettings(
    val safeSearchRadius: Int,
    val createVoidSafetyPlatform: Boolean,
)

/** Coordinates routing, destination resolution, dimension transfer, and platform rollback. */
internal class TeleporterService(
    settings: TeleporterSettings,
) {
    private val destinationResolver =
        TeleporterDestinationResolver(
            horizontalRadius = settings.safeSearchRadius,
            createVoidSafetyPlatform = settings.createVoidSafetyPlatform,
        )

    fun teleport(
        player: ServerPlayer,
        oldData: TeleporterData,
    ): TeleporterResult {
        val leavingVoid = player.level().dimension() == VoidWorldKeys.voidLevel
        val capturedLocation = player.captureLocation()
        val currentLocation =
            if (leavingVoid) {
                capturedLocation
            } else {
                destinationResolver
                    .resolve(player.serverLevel(), capturedLocation, allowPlatform = false)
                    ?.location
                    ?: return TeleporterResult.Failure(NO_SAFE_RETURN)
            }

        val route =
            TeleporterRouting.select(
                leavingVoid = leavingVoid,
                currentLocation = currentLocation,
                oldData = oldData,
                defaultReturn = player.defaultReturnLocation(),
                defaultTarget = player.defaultVoidLocation(),
            )
        val requested = route.requestedDestination
        val targetLevel =
            player.server.getLevel(requested.dimension)
                ?: return TeleporterResult.Failure(DIMENSION_MISSING)
        val destination =
            destinationResolver.resolve(
                level = targetLevel,
                requested = requested,
                allowPlatform = !leavingVoid && requested.dimension == VoidWorldKeys.voidLevel,
            ) ?: return TeleporterResult.Failure(NO_SAFE_DESTINATION)

        if (!player.teleportTo(targetLevel, destination)) {
            destination.platformMutation?.rollback()
            return TeleporterResult.Failure(TRANSITION_FAILED)
        }

        return TeleporterResult.Success(route.completed(destination.location))
    }

    private fun ServerPlayer.captureLocation(): SavedLocation = SavedLocation(level().dimension(), blockPosition(), yRot, xRot)

    private fun ServerPlayer.defaultReturnLocation(): SavedLocation =
        SavedLocation(Level.OVERWORLD, server.overworld().sharedSpawnPos, yRot, xRot)

    private fun ServerPlayer.defaultVoidLocation(): SavedLocation {
        val level = server.getLevel(VoidWorldKeys.voidLevel)
        val generator = level?.chunkSource?.generator as? GridChunkGenerator
        val landingY = generator?.settings?.layerHeight?.plus(1) ?: DEFAULT_VOID_Y
        return SavedLocation(VoidWorldKeys.voidLevel, BlockPos(0, landingY, 0), yRot, xRot)
    }

    private fun ServerPlayer.teleportTo(
        targetLevel: ServerLevel,
        destination: ResolvedDestination,
    ): Boolean {
        val targetChunk = ChunkPos(BlockPos.containing(destination.position))
        targetLevel.chunkSource.addRegionTicket(TicketType.POST_TELEPORT, targetChunk, 1, id)
        val result =
            changeDimension(
                DimensionTransition(
                    targetLevel,
                    destination.position,
                    Vec3.ZERO,
                    destination.location.yaw,
                    destination.location.pitch,
                    false,
                    DimensionTransition.PLAY_PORTAL_SOUND,
                ),
            )
        if (result == null) return false
        deltaMovement = Vec3.ZERO
        fallDistance = 0.0f
        return true
    }

    private companion object {
        const val DEFAULT_VOID_Y = 129
        const val NO_SAFE_RETURN = "message.lazy.teleporter.no_safe_return"
        const val DIMENSION_MISSING = "message.lazy.teleporter.dimension_missing"
        const val NO_SAFE_DESTINATION = "message.lazy.teleporter.no_safe_destination"
        const val TRANSITION_FAILED = "message.lazy.teleporter.transition_failed"
    }
}

internal sealed interface TeleporterResult {
    data class Success(
        val newData: TeleporterData,
    ) : TeleporterResult

    data class Failure(
        val translationKey: String,
    ) : TeleporterResult
}
