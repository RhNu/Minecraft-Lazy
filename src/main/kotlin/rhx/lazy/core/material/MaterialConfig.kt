package rhx.lazy.core.material

import net.neoforged.fml.ModContainer
import rhx.lazy.core.config.LazyConfigBuilder
import rhx.lazy.core.config.LazyConfigDefinition

internal class MaterialConfig(
    builder: LazyConfigBuilder,
) {
    val modPriority =
        builder.stringList(
            "modPriority",
            listOf("kubejs", "minecraft", "alltheores", "create", "mekanism", "jaopca"),
            "Preferred item namespaces when a c: material tag holds more than one candidate, " +
                "from highest to lowest priority. Namespaces outside this list fall back to " +
                "ascending namespace and item id.",
        )
}

/**
 * The one place a `c:` material tag turns into a single chosen item.
 *
 * This used to live inside the simulation chamber. It is shared now because the shaper needs the
 * same answer for the same tag: two machines that disagreed about which mod's iron plate is "the"
 * iron plate would be a bug nobody could configure their way out of.
 */
internal object MaterialConfigs {
    const val FILE_NAME = "lazy-material.toml"

    private val definition = LazyConfigDefinition.create(::MaterialConfig)
    val settings: MaterialConfig = definition.settings

    fun register(container: ModContainer) = definition.registerServer(container, FILE_NAME)
}
