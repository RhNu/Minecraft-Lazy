package rhx.lazy.integration.ubesdelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object UbesDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                crop("garlic", "garlic", "garlic_crop"),
                crop("ginger", "ginger", "ginger_crop"),
                crop("ube", "ube", "ube_crop"),
                RegistryPlantSimulationSpec(
                    "lemongrass",
                    rl("lemongrass_seeds"),
                    listOf(
                        RegistryBlockLootPart(
                            rl("lemongrass_stalk_crop"),
                            mapOf("age" to MAX_INTEGER_PROPERTY, "supporting" to "true"),
                        ),
                        RegistryBlockLootPart(rl("lemongrass_leaf_crop"), mapOf("age" to MAX_INTEGER_PROPERTY)),
                    ),
                ),
            ),
        )
    }

    private fun crop(
        id: String,
        input: String,
        block: String,
    ) = RegistryPlantSimulationSpec(id, rl(input), listOf(RegistryBlockLootPart(rl(block), mapOf("age" to MAX_INTEGER_PROPERTY))))

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private const val NS = "ubesdelight"
}
