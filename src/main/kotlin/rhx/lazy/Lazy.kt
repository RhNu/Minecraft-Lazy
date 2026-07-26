package rhx.lazy

import net.neoforged.bus.api.EventPriority
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rhx.lazy.core.command.LazyCommands
import rhx.lazy.core.registry.LazyRegistries
import rhx.lazy.feature.energy.EnergySourceInteractions
import rhx.lazy.feature.protection.DamageCapHandler
import rhx.lazy.feature.repairer.RepairerConfigs
import rhx.lazy.feature.teleporter.TeleporterConfigs
import rhx.lazy.integration.beyonddimensions.BeyondDimensionsIntegration
import rhx.lazy.integration.curios.CuriosTeleporterIntegration
import rhx.lazy.integration.curios.CuriosTeleporterNetworking
import rhx.lazy.integration.repair.RepairCompatibilities
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

internal const val MOD_ID = "lazy"

@Mod(MOD_ID)
object Lazy {
    internal val logger: Logger = LogManager.getLogger(MOD_ID)

    init {
        TeleporterConfigs.init()
        RepairerConfigs.init()
        LazyRegistries.register(MOD_BUS)
        BeyondDimensionsIntegration.init()
        RepairCompatibilities.init()
        CuriosTeleporterIntegration.registerPredicates()
        MOD_BUS.addListener(CuriosTeleporterNetworking::register)
        NeoForge.EVENT_BUS.addListener(EnergySourceInteractions::onRightClickBlock)
        NeoForge.EVENT_BUS.addListener(LazyCommands::register)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DamageCapHandler::onInvulnerabilityCheck)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DamageCapHandler::onIncomingDamage)
        logger.info("Lazy mod initialized")
    }
}
