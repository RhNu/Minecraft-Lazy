package rhx.lazy.integration.kaleidoscopegrilling

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object KaleidoscopeGrillingPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                crop("canola", "canola_seeds", "canola_crop"),
                crop("houttuynia", "houttuynia", "houttuynia_crop"),
                crop("onion", "onion", "onion_crop"),
                crop("sweet_potato", "sweet_potato", "sweet_potato_crop"),
                RegistryPlantSimulationSpec(
                    "pepper_tree",
                    rl("pepper_sapling"),
                    listOf(
                        RegistryBlockLootPart(rl("pepper_log"), minRolls = 1, maxRolls = 4),
                        RegistryBlockLootPart(
                            rl("pepper_leaves"),
                            mapOf("has_pepper" to "false"),
                            mc("shears"),
                            minRolls = 1,
                            maxRolls = 3,
                        ),
                        RegistryBlockLootPart(rl("pepper_leaves"), mapOf("has_pepper" to "true"), minRolls = 1, maxRolls = 3),
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

    private fun mc(path: String) = ResourceLocation.withDefaultNamespace(path)

    private const val NS = "kaleidoscope_grilling"
}
