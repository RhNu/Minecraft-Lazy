package rhx.lazy.integration.delightoflight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryItemOutputPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object DelightOFlightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                crop("cloud_berry", "cloud_berries", "cloud_berry_bush"),
                crop("thunder_fruit", "thunder_fruit_seeds", "thunder_vine"),
                RegistryPlantSimulationSpec(
                    "cloudshroom_colony",
                    rl("cloudshroom_colony"),
                    listOf(
                        RegistryBlockLootPart(
                            rl("cloudshroom_colony"),
                            mapOf("age" to MAX_INTEGER_PROPERTY, "weather" to "0"),
                        ),
                    ),
                ),
                RegistryPlantSimulationSpec(
                    "lotus",
                    rl("lotus_seeds"),
                    listOf(
                        RegistryBlockLootPart(
                            rl("lotus_flower"),
                            mapOf("flower_age" to MAX_INTEGER_PROPERTY, "high" to "1"),
                        ),
                    ),
                    itemOutputs =
                        listOf(
                            RegistryItemOutputPart(rl("lotus_root")),
                            RegistryItemOutputPart(rl("lotus_leaf")),
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

    private const val NS = "delighto_flight"
}
