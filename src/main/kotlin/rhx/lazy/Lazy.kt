package rhx.lazy

import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rhx.lazy.command.LazyCommands
import rhx.lazy.config.ModConfig
import rhx.lazy.registry.ModRegistries
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

internal const val MOD_ID = "lazy"

@Mod(MOD_ID)
object Lazy {
    internal val logger: Logger = LogManager.getLogger(MOD_ID)

    init {
        ModConfig.init()
        ModRegistries.register(MOD_BUS)
        NeoForge.EVENT_BUS.addListener(LazyCommands::register)
        logger.info("Lazy mod initialized")
    }
}
