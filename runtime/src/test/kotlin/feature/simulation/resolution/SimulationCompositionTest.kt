package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimulationCompositionTest {
    @Test
    fun `tool requirements use unordered one to one matching and ignore extras`() {
        val broad = SimulationToolRequirement.Item(Ingredient.of(Items.DIAMOND, Items.EMERALD))
        val narrow = SimulationToolRequirement.Item(Ingredient.of(Items.DIAMOND))

        assertTrue(simulationToolsMatch(listOf(broad, narrow), listOf(ItemStack(Items.DIAMOND), ItemStack(Items.EMERALD))))
        assertTrue(
            simulationToolsMatch(
                listOf(narrow),
                listOf(ItemStack(Items.STICK), ItemStack(Items.DIAMOND), ItemStack(Items.FLINT_AND_STEEL)),
            ),
        )
        assertTrue(!simulationToolsMatch(listOf(broad, narrow), listOf(ItemStack(Items.DIAMOND), ItemStack.EMPTY)))
    }

    @Test
    fun `block tag tools require a matching block item`() {
        val logs = SimulationToolRequirement.BlockTag(BlockTags.LOGS)

        assertTrue(!simulationToolsMatch(listOf(logs), listOf(ItemStack(Items.STICK))))
    }

    @Test
    fun `more required tools win before equal rank conflicts`() {
        val noTool = explicit("no_tool", 10, Items.COAL)
        val oneTool =
            RecipeHolder(
                id("one_tool"),
                ItemSimulationRecipe(
                    Ingredient.of(Items.WHEAT),
                    priority = 10,
                    itemOutputs = listOf(SimulationItemOutput(ItemStack(Items.DIAMOND))),
                    tools = listOf(SimulationToolRequirement.Item(Ingredient.of(Items.SHEARS))),
                ),
            )

        val selected = selectExplicitSimulation(listOf(noTool, oneTool), ItemStack(Items.WHEAT), listOf(ItemStack(Items.SHEARS)))

        assertTrue(selected is RankedSelection.Selected)
        assertEquals(oneTool.id(), selected.value.id())
    }

    @Test
    fun `recipe rejects a fourth tool condition`() {
        val requirement = SimulationToolRequirement.Item(Ingredient.of(Items.STICK))
        assertTrue(
            runCatching {
                ItemSimulationRecipe(
                    Ingredient.of(Items.WHEAT),
                    itemOutputs = listOf(SimulationItemOutput(ItemStack(Items.WHEAT))),
                    tools = List(4) { requirement },
                )
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }

    @Test
    fun `explicit selection rejects equal priority and tool count`() {
        val low = explicit("z_low", 1, Items.COAL)
        val samePriorityLater = explicit("z_later", 10, Items.EMERALD)
        val samePriorityEarlier = explicit("a_earlier", 10, Items.DIAMOND)

        val selected = selectExplicitSimulation(listOf(low, samePriorityLater, samePriorityEarlier), ItemStack(Items.WHEAT))

        assertTrue(selected is RankedSelection.Conflict)
        val conflict = selected
        assertEquals(setOf(samePriorityEarlier.id(), samePriorityLater.id()), conflict.values.map { it.id() }.toSet())
    }

    @Test
    fun `injections append in recipe id order and cannot create a base`() {
        val base =
            ResolvedSimulation.Item(
                id("base"),
                20,
                listOf(SimulationItemOutput(ItemStack(Items.COAL))),
                emptyList(),
            )
        val later = injection("z_later", Items.EMERALD)
        val earlier = injection("a_earlier", Items.DIAMOND)

        val composed = requireNotNull(composeItemSimulation(base, listOf(later, earlier)))

        assertEquals(listOf(Items.COAL, Items.DIAMOND, Items.EMERALD), composed.itemOutputs.map { it.stack.item })
        assertNull(composeItemSimulation(null, listOf(earlier)))
    }

    @Test
    fun `composition disables recipes over the output limit instead of truncating`() {
        val base =
            ResolvedSimulation.Item(
                id("base"),
                20,
                List(MAX_OUTPUT_ENTRIES) { SimulationItemOutput(ItemStack(Items.COAL)) },
                emptyList(),
            )

        assertNull(composeItemSimulation(base, listOf(injection("extra", Items.DIAMOND))))
    }

    private fun explicit(
        name: String,
        priority: Int,
        output: net.minecraft.world.item.Item,
    ) = RecipeHolder(
        id(name),
        ItemSimulationRecipe(
            Ingredient.of(Items.WHEAT),
            priority = priority,
            itemOutputs = listOf(SimulationItemOutput(ItemStack(output))),
        ),
    )

    private fun injection(
        name: String,
        output: net.minecraft.world.item.Item,
    ) = RecipeHolder(
        id(name),
        ItemSimulationInjectionRecipe(
            Ingredient.of(Items.WHEAT),
            itemOutputs = listOf(SimulationItemOutput(ItemStack(output))),
        ),
    )

    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath("lazy_test", path)
}
