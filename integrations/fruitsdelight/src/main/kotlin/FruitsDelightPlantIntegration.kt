package rhx.lazy.integration.fruitsdelight

import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object FruitsDelightPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            trees.map { (fruit, log) -> tree(fruit, log) } +
                listOf(
                    crop("blueberry", "blueberry_bush", "blueberry_bush"),
                    crop("cranberry", "cranberry", "cranberry_bush"),
                    RegistryPlantSimulationSpec(
                        "lemon",
                        rl("lemon_seeds"),
                        listOf(RegistryBlockLootPart(rl("lemon_tree"), mapOf("age" to MAX_INTEGER_PROPERTY, "half" to "lower"))),
                    ),
                    crop("pineapple", "pineapple_sapling", "pineapple"),
                    RegistryPlantSimulationSpec(
                        "hamimelon",
                        rl("hamimelon_seeds"),
                        listOf(RegistryBlockLootPart(rl("hamimelon"))),
                    ),
                ),
        )
    }

    private fun tree(
        fruit: String,
        log: String,
    ): RegistryPlantSimulationSpec {
        val leaves = rl("${fruit}_leaves")
        val structural =
            if (fruit == "durian") mapOf("leaf" to "leaf") else mapOf("type" to "leaves")
        val mature =
            if (fruit == "durian") mapOf("leaf" to "leaf", "fruit" to "fruits") else mapOf("type" to "fruits")
        return RegistryPlantSimulationSpec(
            "${fruit}_tree",
            rl("${fruit}_sapling"),
            listOf(
                RegistryBlockLootPart(mc(log), minRolls = 1, maxRolls = 4),
                RegistryBlockLootPart(leaves, structural, mc("shears"), minRolls = 1, maxRolls = 3),
                RegistryBlockLootPart(leaves, mature, minRolls = 1, maxRolls = 3),
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

    private val trees =
        listOf(
            "apple" to "oak_log",
            "bayberry" to "spruce_log",
            "durian" to "jungle_log",
            "fig" to "oak_log",
            "hawberry" to "spruce_log",
            "kiwi" to "jungle_log",
            "lychee" to "jungle_log",
            "mango" to "jungle_log",
            "mangosteen" to "oak_log",
            "orange" to "oak_log",
            "peach" to "jungle_log",
            "pear" to "birch_log",
            "persimmon" to "spruce_log",
        )
    private const val NS = "fruitsdelight"
}
