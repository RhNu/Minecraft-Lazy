package rhx.lazy.feature.repairer

import net.neoforged.fml.ModContainer
import rhx.lazy.core.config.LazyConfigBuilder
import rhx.lazy.core.config.LazyConfigDefinition

internal class RepairerConfig(
    builder: LazyConfigBuilder,
) {
    val minimumRepairPercent =
        builder.int(
            "minimumRepairPercent",
            5,
            1..100,
            "Minimum percentage of an item's maximum durability repaired per button press.",
        )

    val maximumRepairPercent =
        builder.int(
            "maximumRepairPercent",
            15,
            1..100,
            "Maximum percentage of an item's maximum durability repaired per button press.",
        )
}

internal object RepairerConfigs {
    private val definition = LazyConfigDefinition.create(::RepairerConfig)
    val settings: RepairerConfig = definition.settings

    fun register(container: ModContainer) = definition.registerServer(container, "lazy-repairer.toml")
}
