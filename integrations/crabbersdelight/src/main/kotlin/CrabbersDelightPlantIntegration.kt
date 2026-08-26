package rhx.lazy.integration.crabbersdelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object CrabbersDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                RegistryPlantSimulationSpec(
                    "palm_tree",
                    rl("palm_sapling"),
                    listOf(
                        RegistryBlockLootPart(rl("palm_log"), minRolls = 1, maxRolls = 4),
                        RegistryBlockLootPart(rl("palm_leaves"), tool = mc("shears"), minRolls = 1, maxRolls = 3),
                        RegistryBlockLootPart(rl("palm_leaves"), minRolls = 1, maxRolls = 3),
                    ),
                ),
            ),
        )
    }

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private fun mc(path: String) = ResourceLocation.withDefaultNamespace(path)

    private const val NS = "crabbersdelight"
}
