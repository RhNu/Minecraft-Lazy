package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import rhx.lazy.core.lazyId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimulationTagsTest {
    @Test
    fun `every automatic source derives its own blacklist tag`() {
        listOf(
            TreeSimulationAdapter.SOURCE to "tree",
            CropSimulationAdapter.SOURCE to "crop",
            PlantSimulationAdapter.SOURCE to "plant",
            TaggedMaterialAdapter.SOURCE to "material",
            lazyId("mystical") to "mystical",
        ).forEach { (source, path) ->
            assertEquals(lazyId("simulation/blacklist/$path"), SimulationTags.sourceBlacklist(source).location, path)
        }
    }

    @Test
    fun `third party sources keep their namespace in the derived path`() {
        val source = ResourceLocation.fromNamespaceAndPath("othermod", "reactor")

        assertEquals(lazyId("simulation/blacklist/othermod/reactor"), SimulationTags.sourceBlacklist(source).location)
    }

    @Test
    fun `shipped tag files back every tag the simulation reads`() {
        listOf(
            SimulationTags.plantTargets,
            SimulationTags.duplicateSelfTargets,
            SimulationTags.weaponTools,
            SimulationTags.incineratorTools,
            SimulationTags.incineratedOutputs,
            SimulationTags.blacklist,
            SimulationTags.sourceBlacklist(TreeSimulationAdapter.SOURCE),
            SimulationTags.sourceBlacklist(CropSimulationAdapter.SOURCE),
            SimulationTags.sourceBlacklist(PlantSimulationAdapter.SOURCE),
            SimulationTags.sourceBlacklist(TaggedMaterialAdapter.SOURCE),
            SimulationTags.sourceBlacklist(lazyId("mystical")),
        ).forEach { tag -> assertResource(tag, "item") }

        assertResource(SimulationTags.entityTargetBlacklist, "entity_type")
    }

    private fun assertResource(
        tag: TagKey<*>,
        directory: String,
    ) {
        val path = "/data/${tag.location.namespace}/tags/$directory/${tag.location.path}.json"
        assertNotNull(javaClass.getResource(path), "Missing tag resource $path")
    }
}
