package rhx.lazy.integration.mooncakedelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object MooncakeDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                RegistryPlantSimulationSpec(
                    "sesame",
                    rl("white_sesame"),
                    listOf(RegistryBlockLootPart(rl("sesame_crop"), mapOf("age" to MAX_INTEGER_PROPERTY))),
                ),
            ),
        )
    }

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private const val NS = "mooncake_delight"
}
