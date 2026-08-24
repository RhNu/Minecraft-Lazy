package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import rhx.lazy.core.lazyId
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
