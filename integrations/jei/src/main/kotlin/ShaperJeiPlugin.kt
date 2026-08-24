package rhx.lazy.integration.jei

import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.material.MaterialForm
import rhx.lazy.core.material.MaterialForms
import rhx.lazy.core.material.MaterialIndex
import rhx.lazy.core.material.MaterialIndexes
import rhx.lazy.feature.shaping.ShaperRegistries
import rhx.lazy.feature.shaping.ShaperTags
import rhx.lazy.feature.shaping.shaperTrade
import rhx.lazy.integration.annotation.LazyFrameworkEntrypoint

@JeiPlugin
@LazyFrameworkEntrypoint(key = "shaper")
internal class ShaperJeiPlugin : IModPlugin {
    private var runtime: IJeiRuntime? = null
    private var recipes: List<ShapingDisplay> = emptyList()
    private var indexChanged = false

    init {
        MaterialIndexes.addListener { index ->
            if (runtime == null) {
                indexChanged = true
            } else {
                replaceRecipes(index)
            }
        }
    }

    override fun getPluginUid(): ResourceLocation = PLUGIN_ID

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        val icon = registration.jeiHelpers.guiHelper.createDrawableItemStack(ItemStack(ShaperRegistries.item.get()))
        registration.addRecipeCategories(ShapingCategory(icon))
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        recipes = displays(MaterialIndexes.current())
        registration.addRecipes(TYPE, recipes)
        indexChanged = false
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addRecipeCatalyst(ShaperRegistries.item.get(), TYPE)
    }

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        runtime = jeiRuntime
        if (indexChanged) replaceRecipes(MaterialIndexes.current())
        indexChanged = false
    }

    override fun onRuntimeUnavailable() {
        runtime = null
    }

    private fun replaceRecipes(index: MaterialIndex) {
        val jeiRuntime = runtime ?: return
        val replacement = displays(index)
        jeiRuntime.recipeManager.hideRecipes(TYPE, recipes)
        jeiRuntime.recipeManager.addRecipes(TYPE, replacement)
        recipes = replacement
    }

    private class ShapingCategory(
        private val icon: IDrawable,
    ) : IRecipeCategory<ShapingDisplay> {
        override fun getRecipeType(): RecipeType<ShapingDisplay> = TYPE

        override fun getTitle(): Component = Component.translatable("jei.lazy.shaping")

        override fun getIcon(): IDrawable = icon

        override fun getWidth(): Int = 108

        override fun getHeight(): Int = 36

        override fun setRecipe(
            builder: IRecipeLayoutBuilder,
            recipe: ShapingDisplay,
            focuses: IFocusGroup,
        ) {
            builder
                .addInputSlot(SAMPLE_X, SLOT_Y)
                .addItemStack(recipe.sample)
                .addRichTooltipCallback { _, tooltip -> tooltip.add(Component.translatable("jei.lazy.shaping.sample")) }
            builder.addInputSlot(INPUT_X, SLOT_Y).addItemStack(recipe.input)
            builder.addOutputSlot(OUTPUT_X, SLOT_Y).addItemStack(recipe.output)
        }

        override fun draw(
            recipe: ShapingDisplay,
            recipeSlotsView: IRecipeSlotsView,
            guiGraphics: GuiGraphics,
            mouseX: Double,
            mouseY: Double,
        ) {
            guiGraphics.vLine(
                SEPARATOR_X,
                SEPARATOR_TOP,
                SEPARATOR_BOTTOM,
                0xFF808080.toInt(),
            )
            guiGraphics.drawString(
                net.minecraft.client.Minecraft
                    .getInstance()
                    .font,
                "→",
                ARROW_X,
                ARROW_Y,
                0xFF606060.toInt(),
                false,
            )
        }
    }

    private data class ShapingDisplay(
        val input: ItemStack,
        val sample: ItemStack,
        val output: ItemStack,
    )

    private companion object {
        const val SAMPLE_X = 5
        const val SEPARATOR_X = 33
        const val SEPARATOR_TOP = 5
        const val SEPARATOR_BOTTOM = 30
        const val INPUT_X = 45
        const val OUTPUT_X = 85
        const val SLOT_Y = 9
        const val ARROW_X = 69
        const val ARROW_Y = 14

        val PLUGIN_ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lazy", "shaper_jei")
        val TYPE = RecipeType(ResourceLocation.fromNamespaceAndPath("lazy", "shaping"), ShapingDisplay::class.java)

        val BASE_FORMS =
            listOf(
                MaterialForms.INGOT,
                MaterialForms.GEM,
                MaterialForms.DUST,
                MaterialForms.RAW_MATERIAL,
            )

        fun displays(index: MaterialIndex): List<ShapingDisplay> =
            index.materials
                .sorted()
                .flatMap { material ->
                    val forms = index.formsOf(material)
                    val base = BASE_FORMS.firstOrNull(forms::containsKey) ?: cheapestForm(index, forms) ?: return@flatMap emptyList()
                    val baseItem = forms.getValue(base)
                    forms.entries
                        .asSequence()
                        .filter { (form) -> form != base }
                        .flatMap { (form, item) ->
                            sequenceOf(
                                display(index, base, baseItem, form, item),
                                display(index, form, item, base, baseItem),
                            ).filterNotNull()
                        }.toList()
                }

        fun cheapestForm(
            index: MaterialIndex,
            forms: Map<net.minecraft.resources.ResourceKey<MaterialForm>, net.minecraft.world.item.Item>,
        ) = forms.keys.minWithOrNull(
            compareBy<net.minecraft.resources.ResourceKey<MaterialForm>>(
                { form -> index.unitsOf(form) ?: Int.MAX_VALUE },
                { form -> form.location().toString() },
            ),
        )

        fun display(
            index: MaterialIndex,
            inputForm: net.minecraft.resources.ResourceKey<MaterialForm>,
            inputItem: net.minecraft.world.item.Item,
            outputForm: net.minecraft.resources.ResourceKey<MaterialForm>,
            outputItem: net.minecraft.world.item.Item,
        ): ShapingDisplay? {
            val input = ItemStack(inputItem)
            val output = ItemStack(outputItem)
            if (input.`is`(ShaperTags.inputBlacklist) || output.`is`(ShaperTags.outputBlacklist)) return null
            val trade = shaperTrade(index.unitsOf(inputForm) ?: return null, index.unitsOf(outputForm) ?: return null) ?: return null
            return ShapingDisplay(
                input.copyWithCount(trade.inputPerTrade.toInt()),
                output.copyWithCount(1),
                output.copyWithCount(trade.outputPerTrade.toInt()),
            )
        }
    }
}
