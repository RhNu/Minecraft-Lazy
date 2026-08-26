package rhx.lazy.integration.kaleidoscopetavern

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import rhx.lazy.feature.simulation.MAX_INTEGER_PROPERTY
import rhx.lazy.feature.simulation.RegistryBlockLootPart
import rhx.lazy.feature.simulation.RegistryPlantSimulationSpec
import rhx.lazy.feature.simulation.SimulationToolRequirement
import rhx.lazy.feature.simulation.registerRegistryPlantSimulations
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object KaleidoscopeTavernPlantIntegration : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        registerRegistryPlantSimulations(
            NS,
            listOf(
                grape("grape", "grape_crop", priority = 400),
                grape("ice_grape", "ice_grape_crop", "can_grow_ice_grape", priority = 500),
                grape("gold_grape", "gold_grape_crop", "can_grow_gold_grape", priority = 500),
            ),
        )
    }

    private fun grape(
        id: String,
        block: String,
        toolTag: String? = null,
        priority: Int,
    ) = RegistryPlantSimulationSpec(
        id,
        rl("grapevine"),
        listOf(RegistryBlockLootPart(rl(block), mapOf("age" to MAX_INTEGER_PROPERTY))),
        priority,
        toolTag?.let { listOf(SimulationToolRequirement.BlockTag(TagKey.create(Registries.BLOCK, rl(it)))) }.orEmpty(),
    )

    private fun rl(path: String) = ResourceLocation.fromNamespaceAndPath(NS, path)

    private const val NS = "kaleidoscope_tavern"
}
