package rhx.lazy.feature.simulation

import net.neoforged.fml.ModContainer
import rhx.lazy.core.config.LazyConfigBuilder
import rhx.lazy.core.config.LazyConfigDefinition

internal class SimulationConfig(
    builder: LazyConfigBuilder,
) {
    val defaultDuration =
        builder.int(
            "defaultDuration",
            1200,
            1..72_000,
            "Default simulation duration in ticks when a recipe does not specify one.",
        )

    val maxRollsPerTick =
        builder.int(
            "maxRollsPerTick",
            16,
            1..4096,
            "Maximum virtual output rolls processed by one ordinary chamber per server tick without a dispenser tool.",
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

    val taggedMaterialModPriority =
        builder.stringList(
            "taggedMaterialModPriority",
            listOf("kubejs", "minecraft", "alltheores", "create", "mekanism", "jaopca"),
            "Preferred item namespaces for tag derived outputs, from highest to lowest priority.",
        )

    val t1SpeedMultiplier = builder.int("t1SpeedMultiplier", 1, 1..1024, "Tier 1 simulation speed multiplier.")
    val t1OutputMultiplier = builder.int("t1OutputMultiplier", 1, 1..1024, "Tier 1 simulation output multiplier.")
    val t2SpeedMultiplier = builder.int("t2SpeedMultiplier", 2, 1..1024, "Tier 2 simulation speed multiplier.")
    val t2OutputMultiplier = builder.int("t2OutputMultiplier", 4, 1..1024, "Tier 2 simulation output multiplier.")
    val t3SpeedMultiplier = builder.int("t3SpeedMultiplier", 6, 1..1024, "Tier 3 simulation speed multiplier.")
    val t3OutputMultiplier = builder.int("t3OutputMultiplier", 12, 1..1024, "Tier 3 simulation output multiplier.")
    val t4SpeedMultiplier = builder.int("t4SpeedMultiplier", 18, 1..1024, "Tier 4 simulation speed multiplier.")
    val t4OutputMultiplier = builder.int("t4OutputMultiplier", 36, 1..1024, "Tier 4 simulation output multiplier.")
}

internal object SimulationConfigs {
    private val definition = LazyConfigDefinition.create(::SimulationConfig)
    val settings: SimulationConfig = definition.settings

    fun register(container: ModContainer) = definition.registerServer(container, "lazy-simulation.toml")
}
