package rhx.lazy.integration.kaleidoscopenether

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object KaleidoscopeNetherPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                crop("soul_pepper", "soul_pepper"),
                crop("poisonous_fruit", "poisonous_fruit"),
                berryVine("crimson_fruit", "weeping_cave_vines"),
                berryVine("warped_fruit", "twisting_cave_vines"),
            ),
        )
    }

    private fun crop(
        id: String,
        block: String,
    ) = RegistryPlantSimulationSpec(id, rl(id), listOf(RegistryBlockLootPart(rl(block), mapOf("age" to MAX_INTEGER_PROPERTY))))

    private fun berryVine(
        input: String,
        block: String,
    ) = RegistryPlantSimulationSpec(input, rl(input), listOf(RegistryBlockLootPart(rl(block), mapOf("berries" to "true"))))

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private const val NS = "kaleidoscope_nether"
}
