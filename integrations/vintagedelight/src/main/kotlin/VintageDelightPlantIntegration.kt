package rhx.lazy.integration.vintagedelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object VintageDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                crop("cucumber", "cucumber_seeds", "cucumber_crop"),
                crop("gearo_berry", "gearo_berry", "gearo_berry_bush"),
                crop("ghost_pepper", "ghost_pepper_seeds", "ghost_pepper_crop"),
                crop("oat", "oat_seeds", "oat_crop"),
                crop("peanut", "peanut", "peanut_crop"),
                RegistryPlantSimulationSpec(
                    "magic_vine",
                    rl("magic_peanut"),
                    listOf(
                        RegistryBlockLootPart(rl("magic_vine"), minRolls = 1, maxRolls = 4),
                        RegistryBlockLootPart(rl("magic_peanut"), minRolls = 1, maxRolls = 3),
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

    private const val NS = "vintagedelight"
}
