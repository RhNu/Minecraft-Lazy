package rhx.lazy.integration.kubejs

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin
import dev.latvian.mods.kubejs.recipe.RecipeKey
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext
import dev.latvian.mods.kubejs.recipe.component.BlockStateComponent
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent
import dev.latvian.mods.kubejs.recipe.component.FluidStackComponent
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent
import dev.latvian.mods.kubejs.recipe.component.NumberComponent
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent
import dev.latvian.mods.kubejs.recipe.component.StringComponent
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry
import dev.latvian.mods.kubejs.recipe.schema.function.RecipeFunctionInstance
import dev.latvian.mods.kubejs.recipe.schema.function.ResolvedRecipeSchemaFunction
import dev.latvian.mods.kubejs.script.BindingRegistry
import dev.latvian.mods.kubejs.util.IntBounds
import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_OUTPUT_ENTRIES
import rhx.lazy.feature.simulation.MAX_SIMULATION_TOOLS
import rhx.lazy.integration.annotation.LazyFrameworkEntrypoint

@LazyFrameworkEntrypoint(key = "lazy")
internal class LazyKubeJsPlugin : KubeJSPlugin {
    override fun registerBindings(registry: BindingRegistry) {
        registry.add("Lazy", LazyScriptFacade)
    }

    override fun registerRecipeSchemas(registry: RecipeSchemaRegistry) {
        registry.register(id("item_simulation"), itemSchema())
        registry.register(id("item_simulation_injection"), itemInjectionSchema())
        registry.register(id("entity_simulation"), entitySchema())
    }

    private fun itemInjectionSchema(): RecipeSchema {
        val input = IngredientComponent.INGREDIENT.inputKey("input")
        val group = group()
        val tools = toolRequirements()
        val itemOutputs = boundedOutputs(false).outputKey("item_outputs").optional(emptyList())
        val fluidOutputs = boundedOutputs(true).outputKey("fluid_outputs").optional(emptyList())
        val blockLootOutputs = boundedBlockLootOutputs()
        return RecipeSchema(input, group, tools, itemOutputs, fluidOutputs, blockLootOutputs)
            .constructor(input)
            .toolFunctions(tools)
            .addToListOpFunction("itemOutput", itemOutputs)
            .addToListOpFunction("fluidOutput", fluidOutputs)
            .addToListOpFunction("blockLootOutput", blockLootOutputs)
    }

    private fun itemSchema(): RecipeSchema {
        val input = IngredientComponent.INGREDIENT.inputKey("input")
        val group = group()
        val duration =
            NumberComponent.POSITIVE_INT
                .otherKey("duration")
                .defaultOptional()
                .functionNames("duration")
        val priority =
            NumberComponent.INT
                .otherKey("priority")
                .optional(0)
                .functionNames("priority")
        val tools = toolRequirements()
        val itemOutputs = boundedOutputs(false).outputKey("item_outputs").optional(emptyList())
        val fluidOutputs = boundedOutputs(true).outputKey("fluid_outputs").optional(emptyList())
        val blockLootOutputs = boundedBlockLootOutputs()
        return RecipeSchema(input, group, duration, priority, tools, itemOutputs, fluidOutputs, blockLootOutputs)
            .constructor(input)
            .toolFunctions(tools)
            .addToListOpFunction("itemOutput", itemOutputs)
            .addToListOpFunction("fluidOutput", fluidOutputs)
            .addToListOpFunction("blockLootOutput", blockLootOutputs)
    }

    private fun entitySchema(): RecipeSchema {
        val entity = StringComponent.ID.inputKey("entity")
        val group = group()
        val duration =
            NumberComponent.POSITIVE_INT
                .otherKey("duration")
                .defaultOptional()
                .functionNames("duration")
        val priority =
            NumberComponent.INT
                .otherKey("priority")
                .optional(0)
                .functionNames("priority")
        val tools = toolRequirements()
        val rollLoot =
            BooleanComponent.BOOLEAN
                .otherKey("roll_loot_table")
                .optional(true)
                .functionNames("rollLootTable")
        val lootTable =
            StringComponent.ID
                .otherKey("loot_table")
                .defaultOptional()
                .functionNames("lootTable")
        val itemOutputs = boundedOutputs(false).outputKey("item_outputs").optional(emptyList())
        val fluidOutputs = boundedOutputs(true).outputKey("fluid_outputs").optional(emptyList())
        val displayItems =
            ItemStackComponent.ITEM_STACK
                .instance()
                .asList()
                .outputKey("display_item_outputs")
                .optional(emptyList())
        val displayFluids =
            FluidStackComponent.FLUID_STACK
                .instance()
                .asList()
                .outputKey("display_fluid_outputs")
                .optional(emptyList())
        return RecipeSchema(
            entity,
            group,
            duration,
            priority,
            tools,
            rollLoot,
            lootTable,
            itemOutputs,
            fluidOutputs,
            displayItems,
            displayFluids,
        ).constructor(entity)
            .toolFunctions(tools)
            .addToListOpFunction("itemOutput", itemOutputs)
            .addToListOpFunction("fluidOutput", fluidOutputs)
            .addToListOpFunction("displayItemOutput", displayItems)
            .addToListOpFunction("displayFluidOutput", displayFluids)
    }

    private fun group(): RecipeKey<String> =
        StringComponent.ID
            .otherKey("group")
            .defaultOptional()
            .functionNames("simulationGroup")

    private fun outputObject(fluid: Boolean): CustomObjectRecipeComponent =
        RecipeComponent.builder(
            CustomObjectRecipeComponent.Key(
                "stack",
                if (fluid) FluidStackComponent.FLUID_STACK.instance() else ItemStackComponent.ITEM_STACK.instance(),
            ),
            CustomObjectRecipeComponent.Key("chance", NumberComponent.floatRange(0f, 1f), true),
            CustomObjectRecipeComponent.Key("min_rolls", NumberComponent.NON_NEGATIVE_INT.instance(), true),
            CustomObjectRecipeComponent.Key("max_rolls", NumberComponent.NON_NEGATIVE_INT.instance(), true),
        )

    private fun boundedOutputs(fluid: Boolean) = outputObject(fluid).asList().withBounds(IntBounds(0, MAX_OUTPUT_ENTRIES))

    private fun toolRequirements(): RecipeKey<List<List<CustomObjectRecipeComponent.Value>>> =
        toolObject()
            .asList()
            .withBounds(IntBounds(0, MAX_SIMULATION_TOOLS))
            .otherKey("tools")
            .optional(emptyList())

    private fun toolObject(): CustomObjectRecipeComponent =
        RecipeComponent.builder(
            CustomObjectRecipeComponent.Key("type", StringComponent.STRING.instance()),
            CustomObjectRecipeComponent.Key("ingredient", IngredientComponent.INGREDIENT.instance(), true),
            CustomObjectRecipeComponent.Key("tag", StringComponent.ID.instance(), true),
        )

    private fun boundedBlockLootOutputs(): RecipeKey<List<List<CustomObjectRecipeComponent.Value>>> =
        RecipeComponent
            .builder(
                CustomObjectRecipeComponent.Key("state", BlockStateComponent.BLOCK.instance()),
                CustomObjectRecipeComponent.Key(
                    "display_items",
                    ItemStackComponent.ITEM_STACK.instance().asList(),
                    true,
                ),
                CustomObjectRecipeComponent.Key("tool", ItemStackComponent.OPTIONAL_ITEM_STACK.instance(), true),
                CustomObjectRecipeComponent.Key("chance", NumberComponent.floatRange(0f, 1f), true),
                CustomObjectRecipeComponent.Key("min_rolls", NumberComponent.NON_NEGATIVE_INT.instance(), true),
                CustomObjectRecipeComponent.Key("max_rolls", NumberComponent.NON_NEGATIVE_INT.instance(), true),
            ).asList()
            .withBounds(IntBounds(0, MAX_OUTPUT_ENTRIES))
            .outputKey("block_loot_outputs")
            .optional(emptyList())

    private fun RecipeSchema.toolFunctions(tools: RecipeKey<List<List<CustomObjectRecipeComponent.Value>>>): RecipeSchema {
        @Suppress("UNCHECKED_CAST")
        val component = tools.component as ListRecipeComponent<List<CustomObjectRecipeComponent.Value>>
        val objectComponent = component.component as CustomObjectRecipeComponent
        val keys = objectComponent.keys()
        return function(
            RecipeFunctionInstance(
                "tool",
                listOf(IngredientComponent.INGREDIENT.instance()),
                appendToolFunction(tools, keys, "item"),
            ),
        ).function(
            RecipeFunctionInstance(
                "blockTagTool",
                listOf(StringComponent.ID.instance()),
                appendToolFunction(tools, keys, "block_tag"),
            ),
        )
    }

    private fun appendToolFunction(
        tools: RecipeKey<List<List<CustomObjectRecipeComponent.Value>>>,
        keys: List<CustomObjectRecipeComponent.Key>,
        type: String,
    ): ResolvedRecipeSchemaFunction =
        object : ResolvedRecipeSchemaFunction {
            override fun arguments(): List<RecipeComponent<*>> = emptyList()

            override fun execute(
                context: RecipeScriptContext,
                arguments: List<Any>,
            ) {
                val valueKey = if (type == "item") keys[1] else keys[2]
                val entry =
                    listOf(
                        CustomObjectRecipeComponent.Value(keys[0], 0, type),
                        CustomObjectRecipeComponent.Value(valueKey, if (type == "item") 1 else 2, arguments.single()),
                    )
                context.recipe().setValue(tools, context.recipe().getValue(tools).orEmpty() + listOf(entry))
            }
        }

    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath("lazy", path)
}

object LazyScriptFacade {
    @Suppress("ktlint:standard:property-naming")
    const val apiVersion: Int = 2
    val simulation: LazySimulationScriptFacade = LazySimulationScriptFacade
}

object LazySimulationScriptFacade {
    @JvmOverloads
    fun item(
        stack: Any,
        chance: Float = 1f,
        minRolls: Int = 1,
        maxRolls: Int = minRolls,
    ): Map<String, Any> {
        validateRange(chance, minRolls, maxRolls)
        return mapOf("stack" to stack, "chance" to chance, "min_rolls" to minRolls, "max_rolls" to maxRolls)
    }

    @JvmOverloads
    fun fluid(
        fluid: String,
        amount: Int,
        chance: Float = 1f,
        minRolls: Int = 1,
        maxRolls: Int = minRolls,
    ): Map<String, Any> {
        require(amount > 0) { "Simulation fluid amount must be positive" }
        validateRange(chance, minRolls, maxRolls)
        return mapOf(
            "stack" to mapOf("id" to fluid, "amount" to amount),
            "chance" to chance,
            "min_rolls" to minRolls,
            "max_rolls" to maxRolls,
        )
    }

    @JvmOverloads
    fun blockLoot(
        state: Any,
        displayItems: List<Any> = emptyList(),
        tool: Any? = null,
        chance: Float = 1f,
        minRolls: Int = 1,
        maxRolls: Int = minRolls,
    ): Map<String, Any> {
        validateRange(chance, minRolls, maxRolls)
        return buildMap {
            put("state", state)
            if (displayItems.isNotEmpty()) put("display_items", displayItems)
            if (tool != null) put("tool", tool)
            put("chance", chance)
            put("min_rolls", minRolls)
            put("max_rolls", maxRolls)
        }
    }

    private fun validateRange(
        chance: Float,
        minRolls: Int,
        maxRolls: Int,
    ) {
        require(chance in 0f..1f) { "Simulation output chance must be in [0, 1]" }
        require(minRolls >= 0) { "Simulation minimum rolls must be non-negative" }
        require(maxRolls >= minRolls) { "Simulation maximum rolls must be at least minimum rolls" }
    }
}
