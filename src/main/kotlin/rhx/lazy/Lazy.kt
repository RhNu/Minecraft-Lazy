package rhx.lazy

import net.neoforged.bus.api.EventPriority
import net.neoforged.fml.ModList
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rhx.lazy.core.MachineWrench
import rhx.lazy.core.command.LazyCommands
import rhx.lazy.core.registry.LazyRegistries
import rhx.lazy.feature.energy.EnergyBatteryInteractions
import rhx.lazy.feature.protection.DamageCapHandler
import rhx.lazy.feature.repairer.RepairerConfigs
import rhx.lazy.feature.simulation.DataModelInteractionHandler
import rhx.lazy.feature.simulation.SimulationConfigs
import rhx.lazy.feature.simulation.SimulationNetworking
import rhx.lazy.feature.simulation.SimulationRecipeReloads
import rhx.lazy.feature.teleporter.TeleporterConfigs
import rhx.lazy.integration.LazyIntegrations
import rhx.lazy.integration.mysticalagriculture.EssenceConverterConfigs
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

internal const val MOD_ID = "lazy"

@Mod(MOD_ID)
object Lazy {
    internal val logger: Logger = LogManager.getLogger(MOD_ID)

    init {
        val modContainer = ModLoadingContext.get().activeContainer
        TeleporterConfigs.register(modContainer)
        RepairerConfigs.register(modContainer)
        SimulationConfigs.register(modContainer)
        if (ModList.get().isLoaded("mysticalagriculture")) {
            EssenceConverterConfigs.register(modContainer)
        }
        net.neoforged.neoforge.common.NeoForgeMod
            .enableMilkFluid()
        LazyRegistries.register(MOD_BUS)
        LazyIntegrations.initialize(MOD_BUS)
        MOD_BUS.addListener(SimulationNetworking::register)
        NeoForge.EVENT_BUS.addListener(LazyCommands::register)
        NeoForge.EVENT_BUS.addListener(SimulationRecipeReloads::onDatapackSync)
        NeoForge.EVENT_BUS.addListener(SimulationRecipeReloads::onServerTick)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, DataModelInteractionHandler::onEntityInteract)
        NeoForge.EVENT_BUS.addListener(MachineWrench::onRightClickBlock)
        NeoForge.EVENT_BUS.addListener(EnergyBatteryInteractions::onLeftClickBlock)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DamageCapHandler::onInvulnerabilityCheck)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DamageCapHandler::onIncomingDamage)
        logger.info("Lazy mod initialized")
    }
}
