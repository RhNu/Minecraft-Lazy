package rhx.compose

import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rhx.compose.registry.ModRegistries
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

internal const val MOD_ID = "compose"

@Mod(MOD_ID)
object Compose {
    internal val logger: Logger = LogManager.getLogger(MOD_ID)

    init {
        ModRegistries.register(MOD_BUS)
        logger.info("Compose mod initialized")
    }
}
