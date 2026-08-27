package rhx.lazy.feature.simulation

import net.neoforged.fml.ModContainer
import rhx.lazy.core.config.LazyConfigBuilder
import rhx.lazy.core.config.LazyConfigDefinition
import rhx.lazy.feature.machine.ProcessingCoreTier
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class SimulationConfig(
    builder: LazyConfigBuilder,
) {
    val defaultDuration =
        builder.int(
            "defaultDuration",
            1200,
            1..72_000,
            "Default simulation duration in ticks when a recipe does not specify one.",
        )

    val rollBudgetPerTick =
        builder.int(
            "rollBudgetPerTick",
            64,
            1..4096,
            "Maximum virtual output rolls processed by one chamber per server tick.",
        )

    val taggedMaterials =
        builder.boolean(
            "taggedMaterials",
            true,
            "Allow automatic simulations from the c: material tag rules and the lazy:simulation/target/duplicate_self tag.",
        )

    val taggedMaterialDuration =
        builder.int(
            "taggedMaterialDuration",
            1200,
            1..72_000,
            "Duration in ticks for automatically inferred tagged material simulations.",
        )

    val t1SpeedMultiplier = builder.int("t1SpeedMultiplier", 1, 1..1024, "Tier 1 simulation speed multiplier.")
    val t1OutputMultiplier = builder.int("t1OutputMultiplier", 1, 1..1024, "Tier 1 simulation output multiplier.")
    val t2SpeedMultiplier = builder.int("t2SpeedMultiplier", 2, 1..1024, "Tier 2 simulation speed multiplier.")
    val t2OutputMultiplier = builder.int("t2OutputMultiplier", 4, 1..1024, "Tier 2 simulation output multiplier.")
    val t3SpeedMultiplier = builder.int("t3SpeedMultiplier", 6, 1..1024, "Tier 3 simulation speed multiplier.")
    val t3OutputMultiplier = builder.int("t3OutputMultiplier", 12, 1..1024, "Tier 3 simulation output multiplier.")
    val t4SpeedMultiplier = builder.int("t4SpeedMultiplier", 18, 1..1024, "Tier 4 simulation speed multiplier.")
    val t4OutputMultiplier = builder.int("t4OutputMultiplier", 36, 1..1024, "Tier 4 simulation output multiplier.")
    val t5SpeedMultiplier = builder.int("t5SpeedMultiplier", 54, 1..1024, "Tier 5 simulation speed multiplier.")
    val t5OutputMultiplier = builder.int("t5OutputMultiplier", 108, 1..1024, "Tier 5 simulation output multiplier.")
}

@LazyInternalApi
public object SimulationConfigs {
    private val definition = LazyConfigDefinition.create(::SimulationConfig)
    val settings: SimulationConfig = definition.settings

    fun register(container: ModContainer) = definition.registerServer(container, "lazy-simulation.toml")
}

internal fun ProcessingCoreTier.simulationSpeedMultiplier(): Int =
    when (this) {
        ProcessingCoreTier.T1 -> SimulationConfigs.settings.t1SpeedMultiplier.get()
        ProcessingCoreTier.T2 -> SimulationConfigs.settings.t2SpeedMultiplier.get()
        ProcessingCoreTier.T3 -> SimulationConfigs.settings.t3SpeedMultiplier.get()
        ProcessingCoreTier.T4 -> SimulationConfigs.settings.t4SpeedMultiplier.get()
        ProcessingCoreTier.T5 -> SimulationConfigs.settings.t5SpeedMultiplier.get()
    }

internal fun ProcessingCoreTier.simulationOutputMultiplier(): Int =
    when (this) {
        ProcessingCoreTier.T1 -> SimulationConfigs.settings.t1OutputMultiplier.get()
        ProcessingCoreTier.T2 -> SimulationConfigs.settings.t2OutputMultiplier.get()
        ProcessingCoreTier.T3 -> SimulationConfigs.settings.t3OutputMultiplier.get()
        ProcessingCoreTier.T4 -> SimulationConfigs.settings.t4OutputMultiplier.get()
        ProcessingCoreTier.T5 -> SimulationConfigs.settings.t5OutputMultiplier.get()
    }
