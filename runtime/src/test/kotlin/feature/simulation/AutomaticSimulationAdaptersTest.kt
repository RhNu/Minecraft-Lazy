package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import rhx.lazy.core.lazyId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
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
    fun `growth candidate sources merge evidence and deduplicate item block pairs`() {
        val sink = AutomaticGrowthCandidateSink()
        sink.add(
            Items.WHEAT_SEEDS,
            Blocks.WHEAT,
            AutomaticGrowthEvidence.BLOCK_ITEM_MAPPING,
        )
        sink.add(
            Items.WHEAT_SEEDS,
            Blocks.WHEAT,
            AutomaticGrowthEvidence.CROP_CLASS,
        )
        sink.addInputEvidence(Items.WHEAT_SEEDS, AutomaticGrowthEvidence.SEED_TAG)

        val candidate = assertNotNull(sink.finish()[Items.WHEAT_SEEDS]).single()
        assertSame(Blocks.WHEAT, candidate.block)
        assertEquals(
            setOf(
                AutomaticGrowthEvidence.BLOCK_ITEM_MAPPING,
                AutomaticGrowthEvidence.CROP_CLASS,
                AutomaticGrowthEvidence.SEED_TAG,
            ),
            candidate.evidence,
        )
    }

    @Test
    fun `built in growth candidate sources reject duplicate registrations`() {
        assertTrue(
            runCatching {
                AutomaticGrowthCandidateSources.register(lazyId("growth/block_item_mapping")) { }
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }

    @Test
    fun `growth candidate sources reject seed mappings without growth structure`() {
        val sink = AutomaticGrowthCandidateSink()
        sink.add(Items.WHEAT_SEEDS, Blocks.STONE, AutomaticGrowthEvidence.BLOCK_ITEM_MAPPING)
        sink.addInputEvidence(Items.WHEAT_SEEDS, AutomaticGrowthEvidence.SEED_TAG)

        assertNull(sink.finish()[Items.WHEAT_SEEDS])
    }

    @Test
    fun `registered crop mapping is discoverable independently from primary block item`() {
        val wheat =
            assertNotNull(AutomaticGrowthCandidateSources.collect()[Items.WHEAT_SEEDS])
                .single { it.block === Blocks.WHEAT }

        assertTrue(AutomaticGrowthEvidence.BLOCK_ITEM_MAPPING in wheat.evidence)
        assertTrue(AutomaticGrowthEvidence.CROP_CLASS in wheat.evidence)
    }

    @Test
    fun `automatic adapters select one base and input claims outrank ordinary priorities`() {
        val high = AutomaticSimulationCandidate(lazyId("high"), lazyId("high/recipe"), 20, 200)
        val claimed = AutomaticSimulationCandidate(lazyId("claimed"), lazyId("claimed/recipe"), 20, 100, claimsInput = true)
        val low = AutomaticSimulationCandidate(lazyId("low"), lazyId("low/recipe"), 20, 50)

        assertSame(high, selectAutomaticSimulationCandidate(listOf(low, high)))
        assertSame(claimed, selectAutomaticSimulationCandidate(listOf(high, claimed, low)))
        assertNull(selectAutomaticSimulationCandidate(emptyList()))
    }

    @Test
    fun `growth target selection prefers productive sustainable harvest variants`() {
        val budding =
            AutomaticGrowthTarget(
                Blocks.WHEAT.defaultBlockState(),
                listOf(ItemStack(Items.WHEAT_SEEDS)),
                setOf(AutomaticGrowthEvidence.CROP_TAG),
            )
        val environmentalVariant =
            AutomaticGrowthTarget(
                Blocks.BEETROOTS.defaultBlockState(),
                listOf(ItemStack(Items.WHEAT)),
                setOf(AutomaticGrowthEvidence.CROP_CLASS, AutomaticGrowthEvidence.CROP_TAG),
            )
        val normal =
            AutomaticGrowthTarget(
                Blocks.CARROTS.defaultBlockState(),
                listOf(ItemStack(Items.WHEAT), ItemStack(Items.WHEAT_SEEDS)),
                setOf(AutomaticGrowthEvidence.CROP_CLASS, AutomaticGrowthEvidence.CROP_TAG),
            )

        assertSame(
            normal,
            selectAutomaticGrowthTarget(Items.WHEAT_SEEDS, listOf(budding, environmentalVariant, normal)),
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
