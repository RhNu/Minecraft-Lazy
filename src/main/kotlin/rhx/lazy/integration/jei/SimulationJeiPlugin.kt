package rhx.lazy.integration.jei

import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter
import mezz.jei.api.ingredients.subtypes.UidContext
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.registration.ISubtypeRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.feature.simulation.AutomaticSimulationClientSnapshot
import rhx.lazy.feature.simulation.AutomaticSimulationDisplay
import rhx.lazy.feature.simulation.DataModelItem
import rhx.lazy.feature.simulation.ResolvedSimulation
import rhx.lazy.feature.simulation.SimulationFluidOutput
import rhx.lazy.feature.simulation.SimulationItemOutput
import rhx.lazy.feature.simulation.SimulationRecipeResolver
import rhx.lazy.feature.simulation.SimulationRegistries

@JeiPlugin
internal class SimulationJeiPlugin : IModPlugin {
    private var runtime: IJeiRuntime? = null
    private var activeAutomaticRecipes: List<ItemDisplay> = emptyList()
    private var pendingAutomaticSnapshot: List<AutomaticSimulationDisplay>? = null

    init {
        AutomaticSimulationClientSnapshot.addListener { displays ->
            val jeiRuntime = runtime
            if (jeiRuntime == null) {
                pendingAutomaticSnapshot = displays
            } else {
                replaceAutomaticRecipes(jeiRuntime, displays)
            }
        }
    }

    override fun getPluginUid(): ResourceLocation = PLUGIN_ID

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        val icon = registration.jeiHelpers.guiHelper.createDrawableItemStack(ItemStack(SimulationRegistries.item.get()))
        registration.addRecipeCategories(ItemCategory(icon), EntityCategory(icon))
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val level = Minecraft.getInstance().level ?: return
        val explicitItems =
            buildList {
                level.recipeManager.getAllRecipesFor(SimulationRegistries.itemRecipeType.get()).forEach { holder ->
                    val representative =
                        holder
                            .value()
                            .input.items
                            .firstOrNull() ?: return@forEach
                    val resolved = SimulationRecipeResolver.resolve(level, representative) as? ResolvedSimulation.Item ?: return@forEach
                    if (resolved.id != holder.id()) return@forEach
                    add(
                        ItemDisplay(
                            holder.value().input,
                            resolved.itemOutputs.map(ItemOutputDisplay::from) +
                                resolved.blockLootOutputs.flatMap { it.displayItems }.map(ItemOutputDisplay::lootTable),
                            resolved.fluidOutputs.map(FluidOutputDisplay::from),
                        ),
                    )
                }
            }
        activeAutomaticRecipes = automaticDisplays(SimulationRecipeResolver.automaticSimulations(level))
        pendingAutomaticSnapshot = null
        registration.addRecipes(ITEM_TYPE, explicitItems + activeAutomaticRecipes)

        val entities =
            level.recipeManager
                .getAllRecipesFor(SimulationRegistries.entityRecipeType.get())
                .map { holder ->
                    val recipe = holder.value()
                    val model = ItemStack(SimulationRegistries.dataModelItem.get())
                    model.set(SimulationRegistries.entityTypeComponent.get(), recipe.entity)
                    val runtimeItems = recipe.itemOutputs.map(ItemOutputDisplay::from)
                    val runtimeFluids = recipe.fluidOutputs.map(FluidOutputDisplay::from)
                    EntityDisplay(
                        model,
                        runtimeItems +
                            recipe.displayItemOutputs
                                .filter { display -> runtimeItems.none { ItemStack.isSameItemSameComponents(it.stack, display) } }
                                .map(ItemOutputDisplay::lootTable),
                        runtimeFluids +
                            recipe.displayFluidOutputs
                                .filter { display ->
                                    runtimeFluids.none { FluidStack.isSameFluidSameComponents(it.stack, display) }
                                }.map(FluidOutputDisplay::lootTable),
                    )
                }
        registration.addRecipes(ENTITY_TYPE, entities)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addRecipeCatalyst(SimulationRegistries.item.get(), ITEM_TYPE, ENTITY_TYPE)
    }

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        runtime = jeiRuntime
        pendingAutomaticSnapshot?.let { replaceAutomaticRecipes(jeiRuntime, it) }
        pendingAutomaticSnapshot = null
    }

    override fun onRuntimeUnavailable() {
        runtime = null
    }

    private fun replaceAutomaticRecipes(
        jeiRuntime: IJeiRuntime,
        displays: List<AutomaticSimulationDisplay>,
    ) {
        val replacement = automaticDisplays(displays)
        jeiRuntime.recipeManager.hideRecipes(ITEM_TYPE, activeAutomaticRecipes)
        jeiRuntime.recipeManager.addRecipes(ITEM_TYPE, replacement)
        activeAutomaticRecipes = replacement
    }

    private fun automaticDisplays(displays: List<AutomaticSimulationDisplay>): List<ItemDisplay> =
        displays.map { automatic ->
            ItemDisplay(
                Ingredient.of(automatic.input),
                automatic.simulation.itemOutputs.map(ItemOutputDisplay::from) +
                    automatic.simulation.blockLootOutputs
                        .flatMap { it.displayItems }
                        .map(ItemOutputDisplay::lootTable),
                automatic.simulation.fluidOutputs.map(FluidOutputDisplay::from),
            )
        }

    override fun registerItemSubtypes(registration: ISubtypeRegistration) {
        registration.registerSubtypeInterpreter(
            SimulationRegistries.dataModelItem.get(),
            object : ISubtypeInterpreter<ItemStack> {
                override fun getSubtypeData(
                    ingredient: ItemStack,
                    context: UidContext,
                ): Any = DataModelItem.entityTypeId(ingredient)?.toString().orEmpty()

                @Deprecated("Required by JEI 19's subtype compatibility interface")
                override fun getLegacyStringSubtypeInfo(
                    ingredient: ItemStack,
                    context: UidContext,
                ): String = DataModelItem.entityTypeId(ingredient)?.toString().orEmpty()
            },
        )
    }

    private class ItemCategory(
        private val icon: IDrawable,
    ) : IRecipeCategory<ItemDisplay> {
        override fun getRecipeType() = ITEM_TYPE

        override fun getTitle(): Component = Component.translatable("jei.lazy.item_simulation")

        override fun getIcon() = icon

        override fun getWidth() = 150

        override fun getHeight() = CATEGORY_HEIGHT

        override fun setRecipe(
            builder: IRecipeLayoutBuilder,
            recipe: ItemDisplay,
            focuses: IFocusGroup,
        ) {
            builder.addInputSlot(INPUT_X, INPUT_Y).addIngredients(recipe.input)
            addOutputs(builder, recipe.items, recipe.fluids)
        }

        override fun draw(
            recipe: ItemDisplay,
            recipeSlotsView: IRecipeSlotsView,
            guiGraphics: GuiGraphics,
            mouseX: Double,
            mouseY: Double,
        ) {
            drawSeparator(guiGraphics)
        }
    }

    private class EntityCategory(
        private val icon: IDrawable,
    ) : IRecipeCategory<EntityDisplay> {
        override fun getRecipeType() = ENTITY_TYPE

        override fun getTitle(): Component = Component.translatable("jei.lazy.entity_simulation")

        override fun getIcon() = icon

        override fun getWidth() = 150

        override fun getHeight() = CATEGORY_HEIGHT

        override fun setRecipe(
            builder: IRecipeLayoutBuilder,
            recipe: EntityDisplay,
            focuses: IFocusGroup,
        ) {
            builder.addInputSlot(INPUT_X, INPUT_Y).addItemStack(recipe.model)
            addOutputs(builder, recipe.items, recipe.fluids)
        }

        override fun draw(
            recipe: EntityDisplay,
            recipeSlotsView: IRecipeSlotsView,
            guiGraphics: GuiGraphics,
            mouseX: Double,
            mouseY: Double,
        ) {
            drawSeparator(guiGraphics)
        }
    }

    private data class ItemDisplay(
        val input: Ingredient,
        val items: List<ItemOutputDisplay>,
        val fluids: List<FluidOutputDisplay>,
    )

    private data class EntityDisplay(
        val model: ItemStack,
        val items: List<ItemOutputDisplay>,
        val fluids: List<FluidOutputDisplay>,
    )

    private data class ItemOutputDisplay(
        val stack: ItemStack,
        val chance: Float?,
        val minRolls: Int,
        val maxRolls: Int,
    ) {
        companion object {
            fun from(output: SimulationItemOutput) = ItemOutputDisplay(output.stack.copy(), output.chance, output.minRolls, output.maxRolls)

            fun lootTable(stack: ItemStack) = ItemOutputDisplay(stack.copy(), null, 0, 0)
        }
    }

    private data class FluidOutputDisplay(
        val stack: FluidStack,
        val chance: Float?,
        val minRolls: Int,
        val maxRolls: Int,
    ) {
        companion object {
            fun from(output: SimulationFluidOutput) =
                FluidOutputDisplay(output.stack.copy(), output.chance, output.minRolls, output.maxRolls)

            fun lootTable(stack: FluidStack) = FluidOutputDisplay(stack.copy(), null, 0, 0)
        }
    }

    companion object {
        private const val CATEGORY_WIDTH = 150
        private const val CATEGORY_HEIGHT = 108
        private const val INPUT_X = 66
        private const val INPUT_Y = 2
        private const val LOOT_X = 9
        private const val LOOT_Y = 30
        private const val GRID_COLUMNS = 7
        private const val SLOT_STEP = 19
        private val PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("lazy", "simulation_jei")
        private val ITEM_TYPE = RecipeType(ResourceLocation.fromNamespaceAndPath("lazy", "item_simulation"), ItemDisplay::class.java)
        private val ENTITY_TYPE = RecipeType(ResourceLocation.fromNamespaceAndPath("lazy", "entity_simulation"), EntityDisplay::class.java)

        private fun addOutputs(
            builder: IRecipeLayoutBuilder,
            items: List<ItemOutputDisplay>,
            fluids: List<FluidOutputDisplay>,
        ) {
            items.forEachIndexed { index, output ->
                builder
                    .addOutputSlot(LOOT_X + index % GRID_COLUMNS * SLOT_STEP, LOOT_Y + index / GRID_COLUMNS * SLOT_STEP)
                    .addItemStack(output.stack)
                    .addRichTooltipCallback { _, tooltip -> tooltip.add(output.tooltip()) }
            }
            fluids.forEachIndexed { fluidIndex, output ->
                val index = items.size + fluidIndex
                builder
                    .addOutputSlot(LOOT_X + index % GRID_COLUMNS * SLOT_STEP, LOOT_Y + index / GRID_COLUMNS * SLOT_STEP)
                    .setFluidRenderer(
                        output.stack.amount
                            .coerceAtLeast(1)
                            .toLong(),
                        false,
                        16,
                        16,
                    ).addFluidStack(output.stack.fluid, output.stack.amount.toLong(), output.stack.componentsPatch)
                    .addRichTooltipCallback { _, tooltip -> tooltip.add(output.tooltip()) }
            }
        }

        private fun ItemOutputDisplay.tooltip(): Component = outputTooltip(chance, minRolls, maxRolls)

        private fun FluidOutputDisplay.tooltip(): Component = outputTooltip(chance, minRolls, maxRolls)

        private fun outputTooltip(
            chance: Float?,
            minRolls: Int,
            maxRolls: Int,
        ): Component =
            if (chance == null) {
                Component.translatable("jei.lazy.simulation.loot_table_output")
            } else {
                Component.translatable("jei.lazy.simulation.output_range", chance * 100f, minRolls, maxRolls)
            }

        private fun drawSeparator(guiGraphics: GuiGraphics) {
            guiGraphics.hLine(5, CATEGORY_WIDTH - 6, 24, 0xFF808080.toInt())
        }
    }
}
