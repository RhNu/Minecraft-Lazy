package rhx.lazy.integration.kaleidoscopeend

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object KaleidoscopeEndPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                RegistryPlantSimulationSpec(
                    "ender_mint",
                    rl("ender_mint"),
                    listOf(RegistryBlockLootPart(rl("ender_mint"), mapOf("age" to MAX_INTEGER_PROPERTY))),
                ),
                RegistryPlantSimulationSpec(
                    "dream_berry",
                    rl("dream_berry"),
                    listOf(RegistryBlockLootPart(rl("dream_berry_head"), mapOf("berries" to "true"))),
                ),
            ),
        )
    }

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private const val NS = "kaleidoscope_end"
}
