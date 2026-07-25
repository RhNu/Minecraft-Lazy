package rhx.lazy.integration.repair

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID
import rhx.lazy.integration.silentgear.SilentGearRepairCompatibility

internal object RepairCompatibilities : ItemRepairCompatibility {
    private const val SILENT_GEAR_MOD_ID = "silentgear"

    private val logger = LogManager.getLogger("$MOD_ID/RepairCompatibilities")
    private val handlers = mutableListOf<ItemRepairCompatibility>()

    fun init() {
        if (ModList.get().isLoaded(SILENT_GEAR_MOD_ID)) {
            try {
                handlers += SilentGearRepairCompatibility
                logger.info("Enabled Silent Gear repair compatibility")
            } catch (error: LinkageError) {
                logger.error("Failed to load Silent Gear repair compatibility", error)
            }
        }
    }

    override fun afterRepair(
        stack: ItemStack,
        player: Player?,
    ) {
        handlers.forEach { handler ->
            try {
                handler.afterRepair(stack, player)
            } catch (exception: RuntimeException) {
                logger.error("Repair compatibility failed for {}", stack.hoverName.string, exception)
            }
        }
    }
}
