package rhx.lazy.core.datagen

import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.loot.packs.VanillaBlockLoot
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.BlockTagsProvider
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.LanguageProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.repairer.RepairerRegistries
import rhx.lazy.feature.teleporter.TeleporterRegistries
import rhx.lazy.feature.voidworld.VoidWorldBootstrap
import rhx.lazy.integration.curios.CuriosTeleporterIntegration
import top.theillusivec4.curios.api.CuriosDataProvider
import java.util.concurrent.CompletableFuture

@EventBusSubscriber(modid = MOD_ID)
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
        generator.addProvider(
            event.includeServer(),
            CuriosData(output, helper, lookup),
        )
        generator.addProvider(
            event.includeServer(),
            DatapackBuiltinEntriesProvider(
                output,
                lookup,
                RegistrySetBuilder()
                    .add(Registries.BIOME, VoidWorldBootstrap::bootstrapBiome)
                    .add(Registries.DIMENSION_TYPE, VoidWorldBootstrap::bootstrapDimensionType)
                    .add(Registries.LEVEL_STEM, VoidWorldBootstrap::bootstrapLevelStem),
                setOf(MOD_ID),
            ),
        )

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
                .shaped(RecipeCategory.MISC, BufferRegistries.item.get())
                .pattern("I I")
                .pattern(" C ")
                .pattern("I I")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.CHEST)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, TeleporterRegistries.item.get())
                .pattern("EEE")
                .pattern("EAE")
                .pattern("EEE")
                .define('E', Tags.Items.ENDER_PEARLS)
                .define('A', ItemTags.ANVIL)
                .unlockedBy("has_ender_pearl", has(Tags.Items.ENDER_PEARLS))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, EnergyRegistries.batteryItem.get())
                .pattern("IGI")
                .pattern("IAI")
                .pattern("IGI")
                .define('I', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('G', Tags.Items.STORAGE_BLOCKS_GOLD)
                .define('A', ItemTags.ANVIL)
                .unlockedBy("has_gold_block", has(Tags.Items.STORAGE_BLOCKS_GOLD))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, EnergyRegistries.sourceItem.get())
                .pattern("III")
                .pattern("GBG")
                .pattern("III")
                .define('I', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('G', Tags.Items.DUSTS_GLOWSTONE)
                .define('B', EnergyRegistries.batteryItem.get())
                .unlockedBy("has_energy_battery", has(EnergyRegistries.batteryItem.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, RepairerRegistries.item.get())
                .pattern("III")
                .pattern(" A ")
                .pattern("CCC")
                .define('I', Tags.Items.INGOTS_COPPER)
                .define('A', ItemTags.ANVIL)
                .define('C', Tags.Items.STORAGE_BLOCKS_COPPER)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output)
        }
    }

    private class ItemModels(
        output: PackOutput,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper,
    ) : ItemModelProvider(output, MOD_ID, helper) {
        override fun registerModels() {
            withExistingParent("buffer", modLoc("block/buffer"))
            basicItem(TeleporterRegistries.item.get())
            basicItem(EnergyRegistries.batteryItem.get())
            withExistingParent("energy_source", modLoc("block/energy_source"))
            withExistingParent("repairer", modLoc("block/repairer"))
        }
    }

    private class BlockStates(
        output: PackOutput,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper,
    ) : BlockStateProvider(output, MOD_ID, helper) {
        override fun registerStatesAndModels() {
            simpleBlock(BufferRegistries.block.get())
            simpleBlock(EnergyRegistries.sourceBlock.get())
            simpleBlock(RepairerRegistries.block.get())
        }
    }

    private class BlockTags(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper?,
    ) : BlockTagsProvider(output, lookup, MOD_ID, helper) {
        override fun addTags(provider: HolderLookup.Provider) {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(
                BufferRegistries.block.get(),
                EnergyRegistries.sourceBlock.get(),
                RepairerRegistries.block.get(),
            )
        }
    }

    private class CuriosData(
        output: PackOutput,
        helper: ExistingFileHelper,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : CuriosDataProvider(MOD_ID, output, helper, lookup) {
        override fun generate(
            registries: HolderLookup.Provider,
            fileHelper: ExistingFileHelper,
        ) {
            createSlot(CuriosTeleporterIntegration.TELEPORTER_SLOT)
                .size(1)
                .icon(
                    ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "slot/empty_teleporter_slot",
                    ),
                ).addValidator(CuriosTeleporterIntegration.teleporterSlotValidator)
            createEntities(CuriosTeleporterIntegration.TELEPORTER_SLOT)
                .addPlayer()
                .addSlots(CuriosTeleporterIntegration.TELEPORTER_SLOT)
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
                add(BufferRegistries.block.get(), noDrop())
                dropSelf(EnergyRegistries.sourceBlock.get())
                dropSelf(RepairerRegistries.block.get())
            }

            override fun getKnownBlocks(): MutableIterable<Block> =
                mutableListOf(
                    BufferRegistries.block.get(),
                    EnergyRegistries.sourceBlock.get(),
                    RepairerRegistries.block.get(),
                )
        }
    }

    private class EnglishLanguage(
        output: PackOutput,
    ) : LanguageProvider(output, MOD_ID, "en_us") {
        override fun addTranslations() {
            addBlock({ BufferRegistries.block.get() }, "Buffer")
            addBlock({ EnergyRegistries.sourceBlock.get() }, "Energy Source")
            addBlock({ RepairerRegistries.block.get() }, "Repairer")
            addItem({ TeleporterRegistries.item.get() }, "Teleporter")
            addItem({ EnergyRegistries.batteryItem.get() }, "Energy Battery")
            add("biome.lazy.void", "Void")
            add("dimension.lazy.void", "Void")
            add("tab.lazy", "Lazy")
            add("message.lazy.buffer.status", "Buffer: %s / %s items, %s / %s mB")
            add("gui.lazy.buffer.summary", "Items %s / %s  ·  Fluids %s / %s mB")
            add("gui.lazy.buffer.items", "Items")
            add("gui.lazy.buffer.fluids", "Fluids")
            add("gui.lazy.buffer.item_count", "×%s")
            add("gui.lazy.buffer.empty", "Empty")
            add("gui.lazy.buffer.clear", "Clear contents")
            add("gui.lazy.buffer.confirm.title", "Clear buffer?")
            add("gui.lazy.buffer.confirm.description", "All stored items and fluids will be destroyed.")
            add("gui.lazy.buffer.confirm", "Clear")
            add("gui.lazy.buffer.cancel", "Cancel")
            add("gui.lazy.buffer.unavailable", "Buffer is no longer available")
            add("gui.lazy.repairer.repair", "Repair item")
            add("tooltip.lazy.buffer.contents", "%s / %s items, %s / %s mB")
            add("tooltip.lazy.energy.max_transfer", "Max transfer: %s FE/t")
            add("tooltip.lazy.energy_battery.use", "Sneak-use on an energy-capable block to transfer energy")
            add("tooltip.lazy.energy_source.active_push", "Sneak-use to toggle active push (off by default)")
            add("message.lazy.energy_battery.transfer", "Energy transferred: %s FE")
            add("message.lazy.energy_source.active_push", "Active push: %s")
            add("message.lazy.energy_source.active_push.enabled", "Enabled")
            add("message.lazy.energy_source.active_push.disabled", "Disabled")
            add("message.lazy.rise.not_found", "No open-sky block found above")
            add("message.lazy.rise.player_only", "This command can only be used by players")
            add("message.lazy.rise.success", "Teleported to the surface")
            add("message.lazy.protection.damage_cap.status", "Damage cap: %s (threshold: %s)")
            add("message.lazy.protection.damage_cap.enabled", "Enabled")
            add("message.lazy.protection.damage_cap.disabled", "Disabled")
            add("message.lazy.protection.damage_cap.on", "Damage cap enabled")
            add("message.lazy.protection.damage_cap.off", "Damage cap disabled")
            add("message.lazy.protection.damage_cap.set", "Damage cap threshold set to %s")
            add("message.lazy.protection.damage_cap.reset", "Damage cap settings cleared")
            add("message.lazy.protection.damage_cap.player_only", "This command can only be used by players")
            add("tooltip.lazy.teleporter.return", "Return: %s @ %s, %s, %s")
            add("tooltip.lazy.teleporter.target", "Target: %s @ %s, %s, %s")
            add("message.lazy.teleporter.cooldown", "Teleporter is cooling down")
            add("message.lazy.teleporter.charge_not_full", "Charge not full")
            add("message.lazy.teleporter.dimension_missing", "Teleport failed: target dimension is unavailable")
            add("message.lazy.teleporter.no_safe_destination", "Teleport failed: no safe destination found")
            add("message.lazy.teleporter.no_safe_return", "Teleport failed: the current return point is unsafe")
            add("message.lazy.teleporter.transition_failed", "Teleport failed during dimension transfer")
            add("message.lazy.teleporter.success", "Teleported successfully; cooldown: %s seconds")
            add("curios.identifier.teleporter", "Teleporter")
            add("key.categories.lazy", "Lazy")
            add("key.lazy.teleporter.activate", "Activate Teleporter")
            add("lazy.teleporter", "Teleporter")
            add("lazy.teleporter.desc", "Server-authoritative teleporter settings")
            add("lazy.teleporter.chargeTicks", "Charge time")
            add("lazy.teleporter.chargeTicks.desc", "Ticks the teleporter must be charged before it activates.")
            add("lazy.teleporter.cooldownSeconds", "Cooldown")
            add("lazy.teleporter.cooldownSeconds.desc", "Cooldown in seconds after a successful teleport.")
            add("lazy.teleporter.safeSearchRadius", "Safe search radius")
            add("lazy.teleporter.safeSearchRadius.desc", "Horizontal radius searched for a safe destination.")
            add("lazy.teleporter.createVoidSafetyPlatform", "Create void safety platform")
            add(
                "lazy.teleporter.createVoidSafetyPlatform.desc",
                "Allow the teleporter to add a small platform in the void dimension.",
            )
            add("lazy.repairer", "Repairer")
            add("lazy.repairer.desc", "Server-authoritative repairer settings")
            add("lazy.repairer.minimumRepairPercent", "Minimum repair percentage")
            add(
                "lazy.repairer.minimumRepairPercent.desc",
                "Minimum percentage of an item's maximum durability repaired per button press.",
            )
            add("lazy.repairer.maximumRepairPercent", "Maximum repair percentage")
            add(
                "lazy.repairer.maximumRepairPercent.desc",
                "Maximum percentage of an item's maximum durability repaired per button press.",
            )
        }
    }

    private class ChineseLanguage(
        output: PackOutput,
    ) : LanguageProvider(output, MOD_ID, "zh_cn") {
        override fun addTranslations() {
            addBlock({ BufferRegistries.block.get() }, "缓冲器")
            addBlock({ EnergyRegistries.sourceBlock.get() }, "能量源")
            addBlock({ RepairerRegistries.block.get() }, "修复器")
            addItem({ TeleporterRegistries.item.get() }, "传送器")
            addItem({ EnergyRegistries.batteryItem.get() }, "能量电池")
            add("biome.lazy.void", "虚空")
            add("dimension.lazy.void", "虚空")
            add("tab.lazy", "Lazy")
            add("message.lazy.buffer.status", "缓冲器：物品 %s / %s，流体 %s / %s mB")
            add("gui.lazy.buffer.summary", "物品 %s / %s  ·  流体 %s / %s mB")
            add("gui.lazy.buffer.items", "物品")
            add("gui.lazy.buffer.fluids", "流体")
            add("gui.lazy.buffer.item_count", "×%s")
            add("gui.lazy.buffer.empty", "空")
            add("gui.lazy.buffer.clear", "清空内容")
            add("gui.lazy.buffer.confirm.title", "清空缓冲器？")
            add("gui.lazy.buffer.confirm.description", "其中的全部物品和流体都会被销毁。")
            add("gui.lazy.buffer.confirm", "确认清空")
            add("gui.lazy.buffer.cancel", "取消")
            add("gui.lazy.buffer.unavailable", "缓冲器已不可用")
            add("gui.lazy.repairer.repair", "修复物品")
            add("tooltip.lazy.buffer.contents", "物品 %s / %s，流体 %s / %s mB")
            add("tooltip.lazy.energy.max_transfer", "最大传输：%s FE/t")
            add("tooltip.lazy.energy_battery.use", "潜行对支持能量的方块使用以传输能量")
            add("tooltip.lazy.energy_source.active_push", "潜行使用以切换主动推送（默认关闭）")
            add("message.lazy.energy_battery.transfer", "已传输能量：%s FE")
            add("message.lazy.energy_source.active_push", "主动推送：%s")
            add("message.lazy.energy_source.active_push.enabled", "已开启")
            add("message.lazy.energy_source.active_push.disabled", "已关闭")
            add("message.lazy.rise.not_found", "未找到上方可见天空的位置")
            add("message.lazy.rise.player_only", "该命令只能由玩家执行")
            add("message.lazy.rise.success", "已传送到地表")
            add("message.lazy.protection.damage_cap.status", "伤害上限：%s（阈值：%s）")
            add("message.lazy.protection.damage_cap.enabled", "已开启")
            add("message.lazy.protection.damage_cap.disabled", "已关闭")
            add("message.lazy.protection.damage_cap.on", "伤害上限已开启")
            add("message.lazy.protection.damage_cap.off", "伤害上限已关闭")
            add("message.lazy.protection.damage_cap.set", "伤害上限阈值已设为 %s")
            add("message.lazy.protection.damage_cap.reset", "伤害上限设置已清除")
            add("message.lazy.protection.damage_cap.player_only", "该命令只能由玩家执行")
            add("tooltip.lazy.teleporter.return", "返回点：%s @ %s, %s, %s")
            add("tooltip.lazy.teleporter.target", "目标点：%s @ %s, %s, %s")
            add("message.lazy.teleporter.cooldown", "传送器冷却中")
            add("message.lazy.teleporter.charge_not_full", "蓄力不足")
            add("message.lazy.teleporter.dimension_missing", "传送失败：目标维度不可用")
            add("message.lazy.teleporter.no_safe_destination", "传送失败：未找到安全落点")
            add("message.lazy.teleporter.no_safe_return", "传送失败：当前位置不能作为安全返回点")
            add("message.lazy.teleporter.transition_failed", "跨维度传送失败")
            add("message.lazy.teleporter.success", "传送成功；冷却 %s 秒")
            add("curios.identifier.teleporter", "传送器")
            add("key.categories.lazy", "懒狗工具箱")
            add("key.lazy.teleporter.activate", "激活传送器")
            add("lazy.teleporter", "传送器")
            add("lazy.teleporter.desc", "由服务器控制的传送器设置")
            add("lazy.teleporter.chargeTicks", "蓄力时间")
            add("lazy.teleporter.chargeTicks.desc", "传送器激活前需要蓄力的游戏刻数。")
            add("lazy.teleporter.cooldownSeconds", "冷却时间")
            add("lazy.teleporter.cooldownSeconds.desc", "成功传送后的冷却秒数。")
            add("lazy.teleporter.safeSearchRadius", "安全搜索半径")
            add("lazy.teleporter.safeSearchRadius.desc", "搜索安全落点时使用的水平半径。")
            add("lazy.teleporter.createVoidSafetyPlatform", "创建虚空安全平台")
            add("lazy.teleporter.createVoidSafetyPlatform.desc", "允许传送器在虚空维度补建小型平台。")
            add("lazy.repairer", "修复器")
            add("lazy.repairer.desc", "由服务端控制的修复器设置")
            add("lazy.repairer.minimumRepairPercent", "最低修复百分比")
            add(
                "lazy.repairer.minimumRepairPercent.desc",
                "每次按下按钮时，最少修复物品最大耐久的百分比。",
            )
            add("lazy.repairer.maximumRepairPercent", "最高修复百分比")
            add(
                "lazy.repairer.maximumRepairPercent.desc",
                "每次按下按钮时，最多修复物品最大耐久的百分比。",
            )
        }
    }
}
