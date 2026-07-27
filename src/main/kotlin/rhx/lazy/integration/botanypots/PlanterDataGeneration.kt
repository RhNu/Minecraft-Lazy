package rhx.lazy.integration.botanypots

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
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.conditions.ModLoadedCondition
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.JsonCodecProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import rhx.lazy.feature.machine.MachineCasingRegistries
import java.util.concurrent.CompletableFuture

internal object PlanterDataGeneration {
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val lookup = event.lookupProvider
        val helper = event.existingFileHelper

        generator.addProvider(
            event.includeServer(),
            NamedProvider("Planter Recipes", PlanterRecipes(output, lookup)),
        )
        generator.addProvider(
            event.includeServer(),
            NamedProvider("Planter Loot Tables", PlanterLootTables(output, lookup, helper)),
        )
        generator.addProvider(
            event.includeClient(),
            NamedProvider("Planter Block States", PlanterBlockStates(output, helper)),
        )
        generator.addProvider(
            event.includeClient(),
            NamedProvider("Planter Item Models", PlanterItemModels(output, helper)),
        )
    }

    private class NamedProvider(
        private val providerName: String,
        private val delegate: DataProvider,
    ) : DataProvider {
        override fun run(output: CachedOutput): CompletableFuture<*> = delegate.run(output)

        override fun getName(): String = providerName
    }

    private class PlanterRecipes(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : RecipeProvider(output, lookup) {
        override fun buildRecipes(output: RecipeOutput) {
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, PlanterRegistries.item.get())
                .pattern(" P ")
                .pattern("HMH")
                .pattern(" C ")
                .define('P', Items.PISTON)
                .define('H', Items.HOPPER)
                .define('C', Items.CHEST)
                .define('M', MachineCasingRegistries.item.get())
                .unlockedBy("has_machine_casing", has(MachineCasingRegistries.item.get()))
                .save(output.withConditions(ModLoadedCondition(BotanyPotsIntegrationModule.modId)))
        }
    }

    private class PlanterBlockStates(
        output: PackOutput,
        helper: ExistingFileHelper,
    ) : BlockStateProvider(output, MOD_ID, helper) {
        override fun registerStatesAndModels() {
            val block = PlanterRegistries.block.get()
            simpleBlock(
                block,
                models().cubeColumn(
                    BuiltInRegistries.BLOCK.getKey(block).path,
                    blockTexture(block),
                    modLoc("block/machine_casing"),
                ),
            )
        }
    }

    private class PlanterItemModels(
        output: PackOutput,
        helper: ExistingFileHelper,
    ) : ItemModelProvider(output, MOD_ID, helper) {
        override fun registerModels() {
            withExistingParent("planter", modLoc("block/planter"))
        }
    }

    private class PlanterLootTables(
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
            val id = ResourceLocation.fromNamespaceAndPath(MOD_ID, "blocks/planter")
            val table =
                LootTable
                    .lootTable()
                    .setParamSet(LootContextParamSets.BLOCK)
                    .setRandomSequence(id)
                    .withPool(
                        LootPool
                            .lootPool()
                            .setRolls(ConstantValue.exactly(1f))
                            .add(
                                LootItem
                                    .lootTableItem(PlanterRegistries.block.get())
                                    .`when`(ExplosionCondition.survivesExplosion()),
                            ),
                    ).build()
            conditionally(id) { builder ->
                builder
                    .withCarrier(table)
                    .addCondition(ModLoadedCondition(BotanyPotsIntegrationModule.modId))
            }
        }
    }
}
