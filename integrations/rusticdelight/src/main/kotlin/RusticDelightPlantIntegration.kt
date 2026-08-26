package rhx.lazy.integration.rusticdelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object RusticDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                crop("bell_pepper", "bell_pepper_seeds", "bell_peppers"),
                crop("dark_bell_pepper", "dark_bell_pepper_seeds", "dark_bell_peppers"),
                crop("pale_bell_pepper", "pale_bell_pepper_seeds", "pale_bell_peppers"),
                crop("coffee", "coffee_beans", "coffee"),
                crop("cotton", "cotton_seeds", "cotton"),
            ),
        )
    }

    private fun crop(
        id: String,
        input: String,
        block: String,
    ) = RegistryPlantSimulationSpec(id, rl(input), listOf(RegistryBlockLootPart(rl(block), mapOf("age" to MAX_INTEGER_PROPERTY))))

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private const val NS = "rusticdelight"
}
