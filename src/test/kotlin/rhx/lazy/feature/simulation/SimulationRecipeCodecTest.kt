package rhx.lazy.feature.simulation

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationRecipeCodecTest {
    @Test
    fun `bundled simulation recipes decode`() {
        val root = Path.of(System.getProperty("lazy.projectDir"), "src/generated/resources/data/lazy/recipe/simulation")
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }.forEach { path ->
                val json = JsonParser.parseString(Files.readString(path)).asJsonObject
                val result =
                    when (json.remove("type").asString) {
                        "lazy:item_simulation" -> ItemSimulationRecipe.CODEC.codec().parse(JsonOps.INSTANCE, json)
                        "lazy:entity_simulation" -> EntitySimulationRecipe.CODEC.codec().parse(JsonOps.INSTANCE, json)
                        else -> error("Unexpected recipe type in $path")
                    }
                assertTrue(result.isSuccess, "$path: ${result.error().orElse(null)}")
            }
        }
    }

    @Test
    fun `entity profile codec applies defaults`() {
        val json = JsonParser.parseString("""{"entity":"minecraft:cow"}""")
        val recipe =
            EntitySimulationRecipe.CODEC
                .codec()
                .parse(JsonOps.INSTANCE, json)
                .result()
                .orElseThrow()

        assertEquals(ResourceLocation.withDefaultNamespace("cow"), recipe.entity)
        assertEquals(ItemSimulationRecipe.USE_CONFIG_DEFAULT, recipe.duration)
        assertEquals(SimulationConfigs.settings.defaultDuration.get(), recipe.durationTicks())
        assertTrue(recipe.rollLootTable)
    }

    @Test
    fun `unprofiled entity uses a valid synthetic recipe id`() {
        val entityId = ResourceLocation.withDefaultNamespace("cow")
        val simulation = ResolvedSimulation.EntityProfile(entityId, null, 1200)

        assertEquals(ResourceLocation.fromNamespaceAndPath("lazy", "entity/minecraft/cow"), simulation.id)
    }

    @Test
    fun `item output codec round trips ranges`() {
        val output = SimulationItemOutput(ItemStack(Items.DIAMOND, 2), 0.25f, 3, 7)
        val encoded =
            SimulationItemOutput.CODEC
                .codec()
                .encodeStart(JsonOps.INSTANCE, output)
                .result()
                .orElseThrow()
        val decoded =
            SimulationItemOutput.CODEC
                .codec()
                .parse(JsonOps.INSTANCE, encoded)
                .result()
                .orElseThrow()

        assertEquals(2, decoded.stack.count)
        assertEquals(0.25f, decoded.chance)
        assertEquals(3..7, decoded.minRolls..decoded.maxRolls)
    }

    @Test
    fun `invalid output probability and range are rejected`() {
        assertTrue(runCatching { SimulationItemOutput(ItemStack(Items.DIAMOND), 1.1f) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(
            runCatching {
                SimulationItemOutput(ItemStack(Items.DIAMOND), minRolls = 4, maxRolls = 3)
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }

    @Test
    fun `explicit zero duration is rejected while an omitted duration uses config`() {
        val invalid =
            JsonParser.parseString(
                """{"input":{"item":"minecraft:diamond"},"duration":0,"item_outputs":[{"stack":{"id":"minecraft:diamond"}}]}""",
            )
        val omitted =
            JsonParser.parseString(
                """{"input":{"item":"minecraft:diamond"},"item_outputs":[{"stack":{"id":"minecraft:diamond"}}]}""",
            )

        assertTrue(
            ItemSimulationRecipe.CODEC
                .codec()
                .parse(JsonOps.INSTANCE, invalid)
                .isError,
        )
        val recipe =
            ItemSimulationRecipe.CODEC
                .codec()
                .parse(JsonOps.INSTANCE, omitted)
                .result()
                .orElseThrow()
        assertEquals(SimulationConfigs.settings.defaultDuration.get(), recipe.durationTicks())
    }

    @Test
    fun `batch snapshots recipe outputs instead of retaining the recipe holder`() {
        val original = SimulationItemOutput(ItemStack(Items.DIAMOND), maxRolls = 2)
        val holder =
            net.minecraft.world.item.crafting.RecipeHolder(
                ResourceLocation.fromNamespaceAndPath("lazy_test", "snapshot"),
                ItemSimulationRecipe(
                    net.minecraft.world.item.crafting.Ingredient
                        .of(Items.WHEAT),
                    itemOutputs = listOf(original),
                ),
            )

        val batch = SimulationBatch.from(ResolvedSimulation.ItemRecipe(holder), 4) as SimulationBatch.Item
        original.stack.count = 42

        assertEquals(
            1,
            batch.itemOutputs
                .single()
                .stack.count,
        )
        assertEquals(4, batch.remaining)
    }

    @Test
    fun `entity profiles reject more outputs than the JEI grid can display`() {
        val displays = List(MAX_OUTPUT_ENTRIES) { ItemStack(Items.DIAMOND) }

        assertTrue(
            runCatching {
                EntitySimulationRecipe(
                    ResourceLocation.withDefaultNamespace("cow"),
                    itemOutputs = listOf(SimulationItemOutput(ItemStack(Items.EMERALD))),
                    displayItemOutputs = displays,
                )
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }
}
