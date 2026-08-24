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
import net.neoforged.neoforge.common.conditions.ICondition
import net.neoforged.neoforge.common.conditions.ModLoadedCondition
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.JsonCodecProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import rhx.lazy.core.datagen.orientableMachineModel
import rhx.lazy.export.DataGenExports
import rhx.lazy.integration.annotation.LazyDataGenContribution
import java.util.concurrent.CompletableFuture

@LazyDataGenContribution(integrationId = "mysticalagriculture")
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
                    output.withConditions(ModLoadedCondition(MysticalAgricultureDataGenExports.MOD_ID)),
                )
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, MysticalAgricultureDataGenExports.essenceConverterItem())
                .pattern(" I ")
                .pattern("HMH")
                .define('I', masterCrystal)
                .define('H', Items.HOPPER)
                .define('M', DataGenExports.machineCasingItem())
                .unlockedBy("has_machine_casing", has(DataGenExports.machineCasingItem()))
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
            delegate.accept(id, MysticalAgricultureDataGenExports.consumingRecipe(shaped), advancement, *conditions)
        }
    }

    private class BlockStates(
        output: PackOutput,
        helper: ExistingFileHelper,
    ) : BlockStateProvider(output, MOD_ID, helper) {
        override fun registerStatesAndModels() {
            val block = MysticalAgricultureDataGenExports.essenceConverterBlock()
            val name = BuiltInRegistries.BLOCK.getKey(block).path
            val model =
                models().orientableMachineModel(
                    name = name,
                    bottom = modLoc("block/machine/bottom"),
                    side = modLoc("block/machine/side"),
                    top = modLoc("block/machine/top"),
                    overlay = modLoc("block/overlay/essence_converter"),
                )
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
                    .addCondition(ModLoadedCondition(MysticalAgricultureDataGenExports.MOD_ID))
            }
        }
    }

    private val MASTER_INFUSION_CRYSTAL_ID =
        ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "master_infusion_crystal")
}
