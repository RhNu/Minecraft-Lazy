package rhx.lazy

import net.neoforged.bus.api.EventPriority
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rhx.lazy.block.EnergySourceInteractions
import rhx.lazy.command.LazyCommands
import rhx.lazy.config.ModConfig
import rhx.lazy.curios.CuriosIntegration
import rhx.lazy.network.ModNetworking
import rhx.lazy.protection.DamageCapHandler
import rhx.lazy.registry.ModRegistries
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

internal const val MOD_ID = "lazy"

@Mod(MOD_ID)
object Lazy {
    internal val logger: Logger = LogManager.getLogger(MOD_ID)

    init {
        ModConfig.init()
        ModRegistries.register(MOD_BUS)
        CuriosIntegration.registerPredicates()
        MOD_BUS.addListener(ModNetworking::register)
        NeoForge.EVENT_BUS.addListener(EnergySourceInteractions::onRightClickBlock)
        NeoForge.EVENT_BUS.addListener(LazyCommands::register)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DamageCapHandler::onInvulnerabilityCheck)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DamageCapHandler::onIncomingDamage)
        logger.info("Lazy mod initialized")
    }
}
