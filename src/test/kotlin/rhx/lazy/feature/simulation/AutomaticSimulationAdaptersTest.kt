package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
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
import kotlin.test.assertTrue

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
    fun `automatic recipe ids follow their source path`() {
        assertEquals(
            lazyId("automatic/plant/minecraft/poppy"),
            inputId(PlantSimulationAdapter.SOURCE, ItemStack(Items.POPPY)),
        )
        assertEquals(lazyId("automatic/material/gem/amethyst"), automaticId(TaggedMaterialAdapter.SOURCE, "gem", "amethyst"))
        assertEquals(
            lazyId("automatic/material/self/minecraft/coal"),
            automaticId(TaggedMaterialAdapter.SOURCE, "self", "minecraft", "coal"),
        )
    }

    @Test
    fun `built in sources share the external registry and reject duplicates`() {
        listOf(
            TreeSimulationAdapter.SOURCE,
            CropSimulationAdapter.SOURCE,
            PlantSimulationAdapter.SOURCE,
            TaggedMaterialAdapter.SOURCE,
        ).forEach { source ->
            assertTrue(
                runCatching {
                    AutomaticSimulationAdapters.register(source) { _, _ -> null }
                }.exceptionOrNull() is IllegalArgumentException,
                source.toString(),
            )
        }
    }
}
