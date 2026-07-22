package rhx.lazy.datagen

import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.loot.packs.VanillaBlockLoot
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.BlockTagsProvider
import net.neoforged.neoforge.common.data.LanguageProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import rhx.lazy.registry.ModBlocks
import rhx.lazy.registry.ModItems
import java.util.concurrent.CompletableFuture

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
internal object DataGeneration {
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val helper = event.existingFileHelper
        val lookup = event.lookupProvider

        generator.addProvider(event.includeServer(), Recipes(output, lookup))
        generator.addProvider(event.includeServer(), LootTables(output, lookup))
        generator.addProvider(event.includeServer(), BlockTags(output, lookup, helper))

        generator.addProvider(event.includeClient(), BlockStates(output, helper))
        generator.addProvider(event.includeClient(), ItemModels(output, helper))
        generator.addProvider(event.includeClient(), EnglishLanguage(output))
        generator.addProvider(event.includeClient(), ChineseLanguage(output))
    }

    private class Recipes(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : RecipeProvider(output, lookup) {
        override fun buildRecipes(output: RecipeOutput) {
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, ModItems.buffer.get())
                .pattern("I I")
                .pattern(" C ")
                .pattern("I I")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.CHEST)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(output)
        }
    }

    private class ItemModels(
        output: PackOutput,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper,
    ) : ItemModelProvider(output, MOD_ID, helper) {
        override fun registerModels() {
            withExistingParent("buffer", modLoc("block/buffer"))
        }
    }

    private class BlockStates(
        output: PackOutput,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper,
    ) : BlockStateProvider(output, MOD_ID, helper) {
        override fun registerStatesAndModels() {
            simpleBlock(ModBlocks.buffer.get())
        }
    }

    private class BlockTags(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper?,
    ) : BlockTagsProvider(output, lookup, MOD_ID, helper) {
        override fun addTags(provider: HolderLookup.Provider) {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.buffer.get())
        }
    }

    private class LootTables(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : LootTableProvider(
            output,
            emptySet(),
            listOf(SubProviderEntry(::BlockLoot, LootContextParamSets.BLOCK)),
            lookup,
        ) {
        private class BlockLoot(
            registries: HolderLookup.Provider,
        ) : VanillaBlockLoot(registries) {
            override fun generate() {
                add(ModBlocks.buffer.get(), noDrop())
            }

            override fun getKnownBlocks(): MutableIterable<Block> =
                mutableListOf(ModBlocks.buffer.get())
        }
    }

    private class EnglishLanguage(
        output: PackOutput,
    ) : LanguageProvider(output, MOD_ID, "en_us") {
        override fun addTranslations() {
            addBlock({ ModBlocks.buffer.get() }, "Buffer")
            add("tab.lazy", "Lazy")
            add("message.lazy.buffer.status", "Buffer: %s / %s items, %s / %s mB")
            add("gui.lazy.buffer.item_amount", "%s items")
            add("gui.lazy.buffer.fluid_amount", "%s mB")
            add("gui.lazy.buffer.empty", "Empty")
            add("gui.lazy.buffer.clear", "Clear contents")
            add("tooltip.lazy.buffer.contents", "%s / %s items, %s / %s mB")
            add("message.lazy.rise.not_found", "No open-sky block found above")
            add("message.lazy.rise.player_only", "This command can only be used by players")
            add("message.lazy.rise.success", "Teleported to the surface")
        }
    }

    private class ChineseLanguage(
        output: PackOutput,
    ) : LanguageProvider(output, MOD_ID, "zh_cn") {
        override fun addTranslations() {
            addBlock({ ModBlocks.buffer.get() }, "缓冲器")
            add("tab.lazy", "Lazy")
            add("message.lazy.buffer.status", "缓冲器：物品 %s / %s，流体 %s / %s mB")
            add("gui.lazy.buffer.item_amount", "物品 %s")
            add("gui.lazy.buffer.fluid_amount", "%s mB")
            add("gui.lazy.buffer.empty", "空")
            add("gui.lazy.buffer.clear", "清空内容")
            add("tooltip.lazy.buffer.contents", "物品 %s / %s，流体 %s / %s mB")
            add("message.lazy.rise.not_found", "未找到上方可见天空的位置")
            add("message.lazy.rise.player_only", "该命令只能由玩家执行")
            add("message.lazy.rise.success", "已传送到地表")
        }
    }
}
