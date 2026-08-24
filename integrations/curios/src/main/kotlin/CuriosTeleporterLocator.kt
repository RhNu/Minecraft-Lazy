package rhx.lazy.integration.curios

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID
import rhx.lazy.feature.teleporter.TeleporterRegistries
import top.theillusivec4.curios.api.CuriosApi

internal sealed interface EquippedTeleporterResult {
    data class Found(
        val stack: ItemStack,
    ) : EquippedTeleporterResult

    data object NotFound : EquippedTeleporterResult

    data object Failed : EquippedTeleporterResult
}

internal object CuriosTeleporterLocator {
    private val logger = LogManager.getLogger("$MOD_ID/curios")

    fun findEquipped(player: ServerPlayer): EquippedTeleporterResult =
        try {
            val stack =
                CuriosApi
                    .getCuriosInventory(player)
                    .flatMap { inventory ->
                        inventory.findCurio(
                            CuriosIntegrationModule.TELEPORTER_SLOT,
                            CuriosIntegrationModule.TELEPORTER_SLOT_INDEX,
                        )
                    }.map { result -> result.stack() }
                    .filter { candidate -> candidate.item === TeleporterRegistries.item.get() }
                    .orElse(null)
            if (stack == null) EquippedTeleporterResult.NotFound else EquippedTeleporterResult.Found(stack)
        } catch (error: LinkageError) {
            logger.error("Curios equipped teleporter lookup linkage failed", error)
            EquippedTeleporterResult.Failed
        } catch (exception: RuntimeException) {
            logger.error("Curios equipped teleporter lookup failed", exception)
            EquippedTeleporterResult.Failed
        }
}
