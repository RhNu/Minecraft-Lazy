package rhx.lazy.feature.teleporter

import net.neoforged.fml.ModContainer
import rhx.lazy.core.config.LazyConfigBuilder
import rhx.lazy.core.config.LazyConfigDefinition

internal class TeleporterConfig(
    builder: LazyConfigBuilder,
) {
    val chargeTicks =
        builder.int(
            "chargeTicks",
            20,
            1..72_000,
            "Ticks the teleporter must be charged before it activates.",
        )

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

    val createVoidSafetyPlatform =
        builder.boolean(
            "createVoidSafetyPlatform",
            true,
            "Allow the teleporter to add a small platform in the void dimension.",
        )
}

internal object TeleporterConfigs {
    private val definition = LazyConfigDefinition.create(::TeleporterConfig)
    val settings: TeleporterConfig = definition.settings

    fun register(container: ModContainer) = definition.registerServer(container, "lazy-teleporter.toml")
}
