package rhx.lazy.integration.kubejs

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent
import dev.latvian.mods.kubejs.recipe.component.FluidStackComponent
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent
import dev.latvian.mods.kubejs.recipe.component.NumberComponent
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent
import dev.latvian.mods.kubejs.recipe.component.StringComponent
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry
import dev.latvian.mods.kubejs.script.BindingRegistry
import dev.latvian.mods.kubejs.util.IntBounds
import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.simulation.MAX_OUTPUT_ENTRIES
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
        val itemOutputs = boundedOutputs(false).outputKey("item_outputs").optional(emptyList())
        val fluidOutputs = boundedOutputs(true).outputKey("fluid_outputs").optional(emptyList())
        return RecipeSchema(input, itemOutputs, fluidOutputs)
            .constructor(input)
            .addToListOpFunction("itemOutput", itemOutputs)
            .addToListOpFunction("fluidOutput", fluidOutputs)
    }

    private fun itemSchema(): RecipeSchema {
        val input = IngredientComponent.INGREDIENT.inputKey("input")
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
        val itemOutputs = boundedOutputs(false).outputKey("item_outputs").optional(emptyList())
        val fluidOutputs = boundedOutputs(true).outputKey("fluid_outputs").optional(emptyList())
        return RecipeSchema(input, duration, priority, itemOutputs, fluidOutputs)
            .constructor(input)
            .addToListOpFunction("itemOutput", itemOutputs)
            .addToListOpFunction("fluidOutput", fluidOutputs)
    }

    private fun entitySchema(): RecipeSchema {
        val entity = StringComponent.ID.inputKey("entity")
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
        return RecipeSchema(entity, duration, priority, rollLoot, lootTable, itemOutputs, fluidOutputs, displayItems, displayFluids)
            .constructor(entity)
            .addToListOpFunction("itemOutput", itemOutputs)
            .addToListOpFunction("fluidOutput", fluidOutputs)
            .addToListOpFunction("displayItemOutput", displayItems)
            .addToListOpFunction("displayFluidOutput", displayFluids)
    }

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

    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath("lazy", path)
}

object LazyScriptFacade {
    @Suppress("ktlint:standard:property-naming")
    const val apiVersion: Int = 1
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
