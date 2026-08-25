package rhx.lazy.feature.teleporter

import net.neoforged.fml.ModContainer
import rhx.lazy.core.config.LazyConfigBuilder
import rhx.lazy.core.config.LazyConfigDefinition

internal class TeleporterConfig(
    builder: LazyConfigBuilder,
) {
    val cooldownSeconds =
        builder.int(
            "cooldownSeconds",
            5,
            0..3_600,
            "Cooldown in seconds after a successful teleport.",
        )

    val safeSearchRadius =
        builder.int(
            "safeSearchRadius",
            8,
            0..16,
            "Horizontal radius searched for a safe destination.",
        )

    val maxSpacesPerPlayer =
        builder.int(
            "maxSpacesPerPlayer",
            64,
            1..4_096,
            "Maximum number of active encapsulated spaces per player. Operators bypass this limit.",
        )
}

internal object TeleporterConfigs {
    private val definition = LazyConfigDefinition.create(::TeleporterConfig)
    val settings: TeleporterConfig = definition.settings

    fun register(container: ModContainer) = definition.registerServer(container, "lazy-teleporter.toml")
}
