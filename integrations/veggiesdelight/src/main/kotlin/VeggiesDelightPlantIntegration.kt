package rhx.lazy.integration.veggiesdelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object VeggiesDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                crop("bellpepper", "bellpepper_seeds", "bellpepper_crop"),
                crop("broccoli", "broccoli_seeds", "broccoli_crop"),
                crop("cauliflower", "cauliflower_seeds", "cauliflower_crop"),
                crop("garlic", "garlic_clove", "garlic_crop"),
                crop("sweet_potato", "sweet_potato", "sweet_potato_crop"),
                crop("turnip", "turnip_seeds", "turnip_crop"),
                crop("zucchini", "zucchini_seeds", "zucchini_crop"),
            ),
        )
    }

    private fun crop(
        id: String,
        input: String,
        block: String,
    ) = RegistryPlantSimulationSpec(id, rl(input), listOf(RegistryBlockLootPart(rl(block), mapOf("age" to MAX_INTEGER_PROPERTY))))

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private const val NS = "veggiesdelight"
}
