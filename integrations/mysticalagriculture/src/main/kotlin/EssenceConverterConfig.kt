package rhx.lazy.integration.mysticalagriculture

import net.neoforged.fml.ModContainer
import rhx.lazy.core.config.LazyConfigBuilder
import rhx.lazy.core.config.LazyConfigDefinition

internal class EssenceConverterConfig(
    builder: LazyConfigBuilder,
) {
    val maxStoredEssence =
        builder.long(
            "maxStoredEssence",
            DEFAULT_CAPACITY,
            1L..Long.MAX_VALUE,
            "Maximum complete target essences stored by one Essence Converter.",
        )

    companion object {
        const val DEFAULT_CAPACITY = 1_000_000_000_000L
    }
}

internal object EssenceConverterConfigs {
    private val definition = LazyConfigDefinition.create(::EssenceConverterConfig)
    val settings: EssenceConverterConfig = definition.settings

    fun register(container: ModContainer) = definition.registerServer(container, "lazy-essence-converter.toml")
}
