package rhx.lazy.integration.ae2

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID
import rhx.lazy.integration.curios.CuriosIntegrationModule
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosLinkCardSource {
    private val logger = LogManager.getLogger("$MOD_ID/ae2-curios")
    private const val CURIOS_MOD_ID = "curios"

    fun isAvailable(): Boolean = ModList.get().isLoaded(CURIOS_MOD_ID)

    fun findEquippedCard(player: ServerPlayer): ItemStack? =
        try {
            CuriosApi
                .getCuriosInventory(player)
                .flatMap { inventory ->
                    inventory.findCurio(
                        CuriosIntegrationModule.ME_LINK_CARD_SLOT,
                        CuriosIntegrationModule.ME_LINK_CARD_SLOT_INDEX,
                    )
                }.map { result -> result.stack() }
                .filter(Ae2Registries::isLinkCard)
                .orElse(null)
        } catch (error: LinkageError) {
            logger.error("Curios ME Link Card lookup linkage failed", error)
            null
        } catch (exception: RuntimeException) {
            logger.error("Curios ME Link Card lookup failed", exception)
            null
        }
}
