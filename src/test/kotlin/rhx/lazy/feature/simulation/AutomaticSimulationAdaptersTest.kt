package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import rhx.lazy.core.lazyId
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
    fun `double plants resolve their lower half so loot conditions match`() {
        listOf(Items.SUNFLOWER, Items.LILAC, Items.PEONY, Items.ROSE_BUSH, Items.PITCHER_PLANT).forEach { item ->
            val state = assertNotNull(automaticPlantState(ItemStack(item)))
            assertEquals(DoubleBlockHalf.LOWER, state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF))
        }
    }

    @Test
    fun `single plants resolve their default state`() {
        listOf(Items.POPPY, Items.DANDELION, Items.WITHER_ROSE, Items.PINK_PETALS, Items.SPORE_BLOSSOM).forEach { item ->
            val state = assertNotNull(automaticPlantState(ItemStack(item)))
            assertEquals(state.block.defaultBlockState(), state)
        }
    }

    @Test
    fun `plant states skip crops and non block items`() {
        listOf(Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO).forEach { item ->
            assertNull(automaticPlantState(ItemStack(item)))
        }
        assertNull(automaticPlantState(ItemStack(Items.STICK)))
    }

    @Test
    fun `every automatic source maps to its own blacklist tag`() {
        listOf("tree", "crop", "plant", "mineral", "mystical").forEach { source ->
            val tag = assertNotNull(SimulationTags.automaticBlacklist(lazyId(source)), source)
            assertEquals(lazyId("automatic_${source}_blacklist"), tag.location)
        }
        assertNull(SimulationTags.automaticBlacklist(lazyId("unknown")))
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

    @Test
    fun `common gem and dust tags expose their material names`() {
        val amethyst = itemTag("gems/amethyst")
        val glowstone = itemTag("dusts/glowstone")

        assertEquals("amethyst", mineralMaterial(amethyst, "gems/"))
        assertEquals("glowstone", mineralMaterial(glowstone, "dusts/"))
        assertNull(mineralMaterial(glowstone, "gems/"))
    }

    private fun id(
        namespace: String,
        path: String,
    ) = ResourceLocation.fromNamespaceAndPath(namespace, path)

    private fun itemTag(path: String): TagKey<Item> = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path))
}
