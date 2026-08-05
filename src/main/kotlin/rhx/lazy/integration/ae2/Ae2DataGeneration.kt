package rhx.lazy.integration.ae2

import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.conditions.ModLoadedCondition
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import java.util.concurrent.CompletableFuture

internal object Ae2DataGeneration {
    fun gatherData(event: GatherDataEvent) {
        event.generator.addProvider(
            event.includeServer(),
            NamedProvider("AE2 Link Card Recipes", Recipes(event.generator.packOutput, event.lookupProvider)),
        )
        event.generator.addProvider(
            event.includeClient(),
            NamedProvider("AE2 Link Card Item Models", ItemModels(event.generator.packOutput, event.existingFileHelper)),
        )
    }

    private class NamedProvider(
        private val providerName: String,
        private val delegate: DataProvider,
    ) : DataProvider {
        override fun run(output: CachedOutput): CompletableFuture<*> = delegate.run(output)

        override fun getName(): String = providerName
    }

    private class Recipes(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : RecipeProvider(output, lookup) {
        override fun buildRecipes(output: RecipeOutput) {
            val memoryCard = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "memory_card"))
            val receiver = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "wireless_receiver"))
            ShapelessRecipeBuilder
                .shapeless(RecipeCategory.MISC, Ae2Registries.meOutputLinkCard.get())
                .requires(memoryCard)
                .requires(receiver)
                .unlockedBy("has_memory_card", has(memoryCard))
                .save(output.withConditions(ModLoadedCondition(Ae2IntegrationModule.modId)))
        }
    }

    private class ItemModels(
        output: PackOutput,
        helper: ExistingFileHelper,
    ) : ItemModelProvider(output, MOD_ID, helper) {
        override fun registerModels() {
            withExistingParent("me_output_link_card", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/icon/me_output_link_card"))
        }
    }
}
