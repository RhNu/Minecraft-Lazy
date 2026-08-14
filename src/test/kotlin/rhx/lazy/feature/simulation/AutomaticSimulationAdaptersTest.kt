package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.CropBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutomaticSimulationAdaptersTest {
    @Test
    fun `vanilla tree names pair logs and leaves including mangrove`() {
        val inputs =
            listOf(
                "oak_sapling" to "oak",
                "spruce_sapling" to "spruce",
                "birch_sapling" to "birch",
                "jungle_sapling" to "jungle",
                "acacia_sapling" to "acacia",
                "dark_oak_sapling" to "dark_oak",
                "cherry_sapling" to "cherry",
                "mangrove_propagule" to "mangrove",
            )

        inputs.forEach { (input, base) ->
            val pair = assertNotNull(automaticTreePair(ResourceLocation.withDefaultNamespace(input)))
            assertEquals(ResourceLocation.withDefaultNamespace("${base}_log"), pair.log)
            assertEquals(ResourceLocation.withDefaultNamespace("${base}_leaves"), pair.leaves)
        }
        assertNull(automaticTreePair(ResourceLocation.withDefaultNamespace("azalea")))
    }

    @Test
    fun `vanilla crop items resolve their mature crop block states`() {
        listOf(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO).forEach { item ->
            val state = assertNotNull(matureCropState(ItemStack(item)))
            val crop = state.block as CropBlock
            assertEquals(crop.maxAge, crop.getAge(state))
        }
        assertNull(matureCropState(ItemStack(Items.SWEET_BERRIES)))
    }

    @Test
    fun `mineral candidate order uses configured namespaces then deterministic fallback`() {
        val comparator = mineralCandidateIdComparator(listOf("kubejs", "minecraft", "create"))
        val ids =
            listOf(
                id("zeta", "raw_iron"),
                id("create", "raw_iron"),
                id("alpha", "raw_iron_b"),
                id("minecraft", "raw_iron"),
                id("kubejs", "raw_iron"),
                id("alpha", "raw_iron_a"),
            ).sortedWith(comparator)

        assertEquals(
            listOf(
                id("kubejs", "raw_iron"),
                id("minecraft", "raw_iron"),
                id("create", "raw_iron"),
                id("alpha", "raw_iron_a"),
                id("alpha", "raw_iron_b"),
                id("zeta", "raw_iron"),
            ),
            ids,
        )
    }

    private fun id(
        namespace: String,
        path: String,
    ) = ResourceLocation.fromNamespaceAndPath(namespace, path)
}
