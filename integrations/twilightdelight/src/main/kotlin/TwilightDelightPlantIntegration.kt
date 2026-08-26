package rhx.lazy.integration.twilightdelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object TwilightDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                RegistryPlantSimulationSpec(
                    "mushgloom_colony",
                    rl("mushgloom_colony"),
                    listOf(RegistryBlockLootPart(rl("mushgloom_colony"), mapOf("age" to MAX_INTEGER_PROPERTY))),
                ),
                RegistryPlantSimulationSpec(
                    "ironwood_tree",
                    rl("ironwood_sapling"),
                    listOf(
                        RegistryBlockLootPart(rl("ironwood_log"), minRolls = 1, maxRolls = 4),
                        RegistryBlockLootPart(rl("ironwood_leaves"), tool = mc("shears"), minRolls = 1, maxRolls = 3),
                        RegistryBlockLootPart(rl("ironwood_leaves"), minRolls = 1, maxRolls = 3),
                    ),
                ),
            ),
        )
    }

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private fun mc(path: String) = ResourceLocation.withDefaultNamespace(path)

    private const val NS = "twilightdelight"
}
