package rhx.lazy.integration.curios

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.core.io.ConfigurationCardSource
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosConfigurationCardSource : ConfigurationCardSource {
    private val logger = LogManager.getLogger("$MOD_ID/configuration-card-curios")

    fun registerValidator() {
        CuriosApi.registerCurioPredicate(CuriosIntegrationModule.configurationCardSlotValidator) { result ->
            ConfigurationCardRegistries.isCard(result.stack())
        }
    }

    override fun findCards(player: ServerPlayer): List<ItemStack> =
        try {
            CuriosApi
                .getCuriosInventory(player)
                .flatMap { inventory ->
                    inventory.findCurio(
                        CuriosIntegrationModule.CONFIGURATION_CARD_SLOT,
                        CuriosIntegrationModule.CONFIGURATION_CARD_SLOT_INDEX,
                    )
                }.map { result -> listOf(result.stack()) }
                .orElse(emptyList())
        } catch (error: LinkageError) {
            logger.error("Curios configuration card lookup linkage failed", error)
            emptyList()
        } catch (exception: RuntimeException) {
            logger.error("Curios configuration card lookup failed", exception)
            emptyList()
        }
}
