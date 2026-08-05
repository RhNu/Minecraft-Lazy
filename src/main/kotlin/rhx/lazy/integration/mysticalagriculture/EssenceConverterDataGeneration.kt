package rhx.lazy.integration.mysticalagriculture

import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.client.model.generators.ModelFile
import net.neoforged.neoforge.common.conditions.ICondition
import net.neoforged.neoforge.common.conditions.ModLoadedCondition
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.JsonCodecProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import rhx.lazy.feature.machine.MachineCasingRegistries
import java.util.concurrent.CompletableFuture

internal object EssenceConverterDataGeneration {
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val lookup = event.lookupProvider
        val helper = event.existingFileHelper

        generator.addProvider(
            event.includeServer(),
            NamedProvider("Essence Converter Recipes", Recipes(output, lookup)),
        )
        generator.addProvider(
            event.includeServer(),
            NamedProvider("Essence Converter Loot Tables", LootTables(output, lookup, helper)),
        )
        generator.addProvider(
            event.includeClient(),
            NamedProvider("Essence Converter Block States", BlockStates(output, helper)),
        )
        generator.addProvider(
            event.includeClient(),
            NamedProvider("Essence Converter Item Models", ItemModels(output, helper)),
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
            val masterCrystal =
                BuiltInRegistries.ITEM
                    .getOptional(MASTER_INFUSION_CRYSTAL_ID)
                    .orElseThrow { IllegalStateException("Mystical Agriculture master infusion crystal is missing") }
            val consumingOutput =
                ConsumingRecipeOutput(
                    output.withConditions(ModLoadedCondition(MysticalAgricultureIntegrationModule.modId)),
                )
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, EssenceConverterRegistries.item.get())
                .pattern(" I ")
                .pattern("HMH")
                .pattern(" C ")
                .define('I', masterCrystal)
                .define('H', Items.HOPPER)
                .define('M', MachineCasingRegistries.item.get())
                .define('C', Items.CHEST)
                .unlockedBy("has_machine_casing", has(MachineCasingRegistries.item.get()))
                .save(consumingOutput)
        }
    }

    private class ConsumingRecipeOutput(
        private val delegate: RecipeOutput,
    ) : RecipeOutput {
        override fun advancement(): Advancement.Builder = delegate.advancement()

        override fun accept(
            id: ResourceLocation,
            recipe: Recipe<*>,
            advancement: AdvancementHolder?,
            vararg conditions: ICondition,
        ) {
            val shaped = recipe as? ShapedRecipe ?: error("Expected a shaped recipe for $id")
            delegate.accept(id, ConsumingShapedRecipe.fromShaped(shaped), advancement, *conditions)
        }
    }

    private class BlockStates(
        output: PackOutput,
        helper: ExistingFileHelper,
    ) : BlockStateProvider(output, MOD_ID, helper) {
        override fun registerStatesAndModels() {
            val block = EssenceConverterRegistries.block.get()
            val model =
                models()
                    .getBuilder("essence_converter")
                    .parent(ModelFile.UncheckedModelFile(modLoc("block/machine_orientable")))
                    .texture("bottom", modLoc("block/machine/bottom"))
                    .texture("side", modLoc("block/machine/side"))
                    .texture("top", modLoc("block/machine/top"))
                    .texture("overlay", modLoc("block/overlay/essence_converter"))
            horizontalBlock(block, model)
        }
    }

    private class ItemModels(
        output: PackOutput,
        helper: ExistingFileHelper,
    ) : ItemModelProvider(output, MOD_ID, helper) {
        override fun registerModels() {
            withExistingParent("essence_converter", modLoc("block/essence_converter"))
        }
    }

    private class LootTables(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
        helper: ExistingFileHelper,
    ) : JsonCodecProvider<LootTable>(
            output,
            PackOutput.Target.DATA_PACK,
            "loot_table",
            PackType.SERVER_DATA,
            LootTable.DIRECT_CODEC,
            lookup,
            MOD_ID,
            helper,
        ) {
        override fun gather() {
            val id = ResourceLocation.fromNamespaceAndPath(MOD_ID, "blocks/essence_converter")
            val table = LootTable.lootTable().setParamSet(LootContextParamSets.BLOCK).build()
            conditionally(id) { builder ->
                builder
                    .withCarrier(table)
                    .addCondition(ModLoadedCondition(MysticalAgricultureIntegrationModule.modId))
            }
        }
    }

    private val MASTER_INFUSION_CRYSTAL_ID =
        ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "master_infusion_crystal")
}
