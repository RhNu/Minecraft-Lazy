package rhx.lazy.integration.farmersdelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object FarmersDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NAMESPACE,
            listOf(
                crop("cabbage", "cabbage_seeds", "cabbages"),
                crop("onion", "onion", "onions"),
                crop("tomato", "tomato_seeds", "tomatoes"),
                crop("rice", "rice", "rice_panicles"),
                crop("red_mushroom_colony", "red_mushroom_colony", "red_mushroom_colony"),
                crop("brown_mushroom_colony", "brown_mushroom_colony", "brown_mushroom_colony"),
            ),
        )
    }

    private fun crop(
        id: String,
        input: String,
        block: String,
    ) = RegistryPlantSimulationSpec(id, rl(input), listOf(RegistryBlockLootPart(rl(block), mapOf("age" to MAX_INTEGER_PROPERTY))))

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NAMESPACE, path)

    private const val NAMESPACE = "farmersdelight"
}
