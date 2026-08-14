package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimulationCompositionTest {
    @Test
    fun `explicit selection uses priority then stable recipe id`() {
        val low = explicit("z_low", 1, Items.COAL)
        val samePriorityLater = explicit("z_later", 10, Items.EMERALD)
        val samePriorityEarlier = explicit("a_earlier", 10, Items.DIAMOND)

        val selected = selectExplicitSimulation(listOf(low, samePriorityLater, samePriorityEarlier), ItemStack(Items.WHEAT))

        assertEquals(samePriorityEarlier.id(), selected?.id())
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

    @Test
    fun `category blacklist routing is source specific`() {
        assertEquals(SimulationTags.automaticTreeBlacklist, SimulationTags.automaticBlacklist(id("tree")))
        assertEquals(SimulationTags.automaticCropBlacklist, SimulationTags.automaticBlacklist(id("crop")))
        assertEquals(SimulationTags.automaticMineralBlacklist, SimulationTags.automaticBlacklist(id("mineral")))
        assertEquals(SimulationTags.automaticMysticalBlacklist, SimulationTags.automaticBlacklist(id("mystical")))
        assertNull(SimulationTags.automaticBlacklist(id("third_party")))
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
