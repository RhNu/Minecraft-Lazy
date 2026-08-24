package rhx.lazy

import net.neoforged.bus.api.EventPriority
import net.neoforged.neoforge.common.NeoForgeMod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rhx.lazy.core.MachineWrench
import rhx.lazy.core.command.LazyCommands
import rhx.lazy.core.material.MaterialConfigs
import rhx.lazy.core.material.MaterialForms
import rhx.lazy.core.material.MaterialIndexReloads
import rhx.lazy.core.registry.LazyRegistries
import rhx.lazy.feature.energy.EnergyBatteryInteractions
import rhx.lazy.feature.protection.DamageCapHandler
import rhx.lazy.feature.repairer.RepairerConfigs
import rhx.lazy.feature.simulation.DataModelInteractionHandler
import rhx.lazy.feature.simulation.SimulationConfigs
import rhx.lazy.feature.simulation.SimulationNetworking
import rhx.lazy.feature.simulation.SimulationRecipeReloads
import rhx.lazy.feature.teleporter.TeleporterConfigs
import rhx.lazy.integration.api.IntegrationCommonContext
import rhx.lazy.integration.api.IntegrationConfigContext
import rhx.lazy.integration.api.LazyInternalApi

/** Phased runtime bootstrap. The distribution entrypoint owns construction of the contexts. */
@LazyInternalApi
public object LazyRuntime {
    internal val logger: Logger = LogManager.getLogger(MOD_ID)

    public fun registerConfig(context: IntegrationConfigContext) {
        TeleporterConfigs.register(context.modContainer)
        RepairerConfigs.register(context.modContainer)
        MaterialConfigs.register(context.modContainer)
        SimulationConfigs.register(context.modContainer)
    }

    public fun install(context: IntegrationCommonContext) {
        NeoForgeMod.enableMilkFluid()
        LazyRegistries.register(context.modBus)
        context.modBus.addListener(MaterialForms::registerDataPackRegistry)
        context.modBus.addListener(MaterialIndexReloads::onConfigLoading)
        context.modBus.addListener(MaterialIndexReloads::onConfigReloading)
        context.modBus.addListener(SimulationNetworking::register)
        context.gameBus.addListener(LazyCommands::register)
        context.gameBus.addListener(SimulationRecipeReloads::onDatapackSync)
        context.gameBus.addListener(SimulationRecipeReloads::onServerTick)
        context.gameBus.addListener(MaterialIndexReloads::onTagsUpdated)
        context.gameBus.addListener(EventPriority.HIGH, DataModelInteractionHandler::onEntityInteract)
        context.gameBus.addListener(MachineWrench::onRightClickBlock)
        context.gameBus.addListener(EnergyBatteryInteractions::onLeftClickBlock)
        context.gameBus.addListener(EventPriority.LOWEST, DamageCapHandler::onInvulnerabilityCheck)
        context.gameBus.addListener(EventPriority.LOWEST, DamageCapHandler::onIncomingDamage)
        logger.info("Lazy runtime initialized")
    }
}
