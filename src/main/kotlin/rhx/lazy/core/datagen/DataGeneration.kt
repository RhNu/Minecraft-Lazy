package rhx.lazy.core.datagen

import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.loot.packs.VanillaBlockLoot
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.LanguageProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.itemcopier.ItemCopierRegistries
import rhx.lazy.feature.machine.MachineCasingRegistries
import rhx.lazy.feature.repairer.RepairerRegistries
import rhx.lazy.feature.simulation.SimulationRecipeData
import rhx.lazy.feature.simulation.SimulationRegistries
import rhx.lazy.feature.teleporter.TeleporterRegistries
import rhx.lazy.feature.voidworld.VoidWorldBootstrap
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
                .shaped(RecipeCategory.BUILDING_BLOCKS, MachineCasingRegistries.item.get())
                .pattern("III")
                .pattern("IRI")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone_dust", has(Tags.Items.DUSTS_REDSTONE))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, BufferRegistries.item.get())
                .pattern(" C ")
                .pattern("IMI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.CHEST)
                .define('M', MachineCasingRegistries.item.get())
                .unlockedBy("has_machine_casing", has(MachineCasingRegistries.item.get()))
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
                .pattern(" B ")
                .pattern("GMG")
                .define('G', Tags.Items.DUSTS_GLOWSTONE)
                .define('B', EnergyRegistries.batteryItem.get())
                .define('M', MachineCasingRegistries.item.get())
                .unlockedBy("has_machine_casing", has(MachineCasingRegistries.item.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, RepairerRegistries.item.get())
                .pattern(" A ")
                .pattern("CMC")
                .define('A', ItemTags.ANVIL)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('M', MachineCasingRegistries.item.get())
                .unlockedBy("has_machine_casing", has(MachineCasingRegistries.item.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, ItemCopierRegistries.item.get())
                .pattern(" C ")
                .pattern("BMB")
                .define('B', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('C', Items.CHEST)
                .define('M', MachineCasingRegistries.item.get())
                .unlockedBy("has_machine_casing", has(MachineCasingRegistries.item.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, SimulationRegistries.coreT1.get())
                .pattern("CRC")
                .pattern("RBR")
                .pattern("CRC")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('B', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, SimulationRegistries.coreT2.get())
                .pattern("IGI")
                .pattern("GTG")
                .pattern("IGI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('T', SimulationRegistries.coreT1.get())
                .unlockedBy("has_t1_core", has(SimulationRegistries.coreT1.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, SimulationRegistries.coreT3.get())
                .pattern("DDD")
                .pattern("DTD")
                .pattern("DDD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('T', SimulationRegistries.coreT2.get())
                .unlockedBy("has_t2_core", has(SimulationRegistries.coreT2.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, SimulationRegistries.coreT4.get())
                .pattern("NNN")
                .pattern("NTN")
                .pattern("NNN")
                .define('N', Tags.Items.INGOTS_NETHERITE)
                .define('T', SimulationRegistries.coreT3.get())
                .unlockedBy("has_t3_core", has(SimulationRegistries.coreT3.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, SimulationRegistries.item.get())
                .pattern("GRG")
                .pattern("HMH")
                .pattern("GRG")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('H', Items.HOPPER)
                .define('M', MachineCasingRegistries.item.get())
                .unlockedBy("has_machine_casing", has(MachineCasingRegistries.item.get()))
                .save(output)
            ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, SimulationRegistries.dataModelItem.get())
                .pattern("PQP")
                .pattern("QRQ")
                .pattern("PQP")
                .define('P', Items.PAPER)
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_quartz", has(Tags.Items.GEMS_QUARTZ))
                .save(output)
            SimulationRecipeData.build(output)
        }
    }

    private class ItemModels(
        output: PackOutput,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper,
    ) : ItemModelProvider(output, MOD_ID, helper) {
        override fun registerModels() {
            withExistingParent("machine_casing", modLoc("block/machine_casing"))
            withExistingParent("buffer", modLoc("block/buffer"))
            withExistingParent("teleporter", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/icon/teleporter"))
            withExistingParent("energy_battery", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/icon/energy_battery"))
            withExistingParent("energy_source", modLoc("block/energy_source"))
            withExistingParent("item_copier", modLoc("block/item_copier"))
            withExistingParent("repairer", modLoc("block/repairer"))
            withExistingParent("simulation_chamber", modLoc("block/simulation_chamber"))
            withExistingParent("data_model", mcLoc("item/generated")).texture("layer0", mcLoc("item/paper"))
            withExistingParent("simulation_core_t1", mcLoc("item/generated")).texture("layer0", mcLoc("item/copper_ingot"))
            withExistingParent("simulation_core_t2", mcLoc("item/generated")).texture("layer0", mcLoc("item/gold_ingot"))
            withExistingParent("simulation_core_t3", mcLoc("item/generated")).texture("layer0", mcLoc("item/diamond"))
            withExistingParent("simulation_core_t4", mcLoc("item/generated")).texture("layer0", mcLoc("item/netherite_ingot"))
        }
    }

    private class BlockStates(
        output: PackOutput,
        helper: net.neoforged.neoforge.common.data.ExistingFileHelper,
    ) : BlockStateProvider(output, MOD_ID, helper) {
        override fun registerStatesAndModels() {
            simpleBlock(
                MachineCasingRegistries.block.get(),
                models().cubeAll("machine_casing", modLoc("block/machine/bottom")),
            )
            machineBlock(BufferRegistries.block.get())
            machineBlock(EnergyRegistries.sourceBlock.get())
            machineBlock(ItemCopierRegistries.block.get())
            machineBlock(RepairerRegistries.block.get())
            machineBlock(SimulationRegistries.block.get())
        }

        private fun machineBlock(
            block: Block,
            overlayName: String = BuiltInRegistries.BLOCK.getKey(block).path,
        ) {
            val name = BuiltInRegistries.BLOCK.getKey(block).path
            val model =
                models().orientableMachineModel(
                    name = name,
                    bottom = modLoc("block/machine/bottom"),
                    side = modLoc("block/machine/side"),
                    top = modLoc("block/machine/top"),
                    overlay = modLoc("block/overlay/$overlayName"),
                )
            horizontalBlock(block, model)
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
                dropSelf(MachineCasingRegistries.block.get())
                add(BufferRegistries.block.get(), noDrop())
                dropSelf(EnergyRegistries.sourceBlock.get())
                add(ItemCopierRegistries.block.get(), noDrop())
                dropSelf(RepairerRegistries.block.get())
                add(SimulationRegistries.block.get(), noDrop())
            }

            override fun getKnownBlocks(): MutableIterable<Block> =
                mutableListOf(
                    MachineCasingRegistries.block.get(),
                    BufferRegistries.block.get(),
                    EnergyRegistries.sourceBlock.get(),
                    ItemCopierRegistries.block.get(),
                    RepairerRegistries.block.get(),
                    SimulationRegistries.block.get(),
                )
        }
    }

    private class EnglishLanguage(
        output: PackOutput,
    ) : LanguageProvider(output, MOD_ID, "en_us") {
        override fun addTranslations() {
            addBlock({ MachineCasingRegistries.block.get() }, "Machine Casing")
            addBlock({ BufferRegistries.block.get() }, "Buffer")
            addBlock({ EnergyRegistries.sourceBlock.get() }, "Energy Source")
            addBlock({ ItemCopierRegistries.block.get() }, "Item Copier")
            addBlock({ RepairerRegistries.block.get() }, "Repairer")
            addBlock({ SimulationRegistries.block.get() }, "Simulation Chamber")
            addItem({ SimulationRegistries.dataModelItem.get() }, "Data Model")
            addItem({ SimulationRegistries.coreT1.get() }, "T1 Simulation Core")
            addItem({ SimulationRegistries.coreT2.get() }, "T2 Simulation Core")
            addItem({ SimulationRegistries.coreT3.get() }, "T3 Simulation Core")
            addItem({ SimulationRegistries.coreT4.get() }, "T4 Simulation Core")
            add("tooltip.lazy.data_model.blank", "Unbound — use on a living entity")
            add("tooltip.lazy.data_model.bound", "Bound to %s — sneak-use to clear")
            add("tooltip.lazy.simulation_core", "Speed ×%s · output ×%s per core")
            add("tooltip.lazy.simulation_chamber.contents", "Contains stored simulation data")
            add("gui.lazy.simulation_chamber.target", "Seed or simulation target")
            add("gui.lazy.simulation_chamber.core", "Simulation core")
            add("gui.lazy.simulation_chamber.progress", "Simulation progress")
            add("gui.lazy.simulation_chamber.pending", "Unable to output — simulation paused")
            add("gui.lazy.simulation_chamber.output_multiplier", "Output multiplier: ×%s")
            add("gui.lazy.simulation_chamber.speed_multiplier", "Speed multiplier: ×%s")
            add("jei.lazy.item_simulation", "Item Simulation")
            add("jei.lazy.entity_simulation", "Entity Simulation")
            add("jei.lazy.simulation.output_range", "Chance: %s%% · rolls: %s–%s")
            add("jei.lazy.simulation.loot_table_output", "Loot table output; amount and chance vary")
            add("config.jade.plugin_lazy.simulation_chamber", "Simulation Chamber status")
            add("jade.lazy.simulation_chamber.progress", "Progress: %s%%")
            add("jade.lazy.simulation_chamber.multipliers", "Speed ×%s · output ×%s")
            add("jade.lazy.simulation_chamber.pending", "Paused: output backlog")
            add("lazy.simulation", "Simulation Chamber")
            add("lazy.simulation.desc", "Server-authoritative simulation chamber settings")
            add("lazy.simulation.defaultDuration", "Default cycle duration")
            add("lazy.simulation.maxRollsPerTick", "Maximum rolls per tick")
            add("lazy.simulation.automaticMinerals", "Automatic mineral recipes")
            add("lazy.simulation.automaticMineralDuration", "Automatic mineral duration")
            add("lazy.simulation.automaticMineralModPriority", "Automatic mineral mod priority")
            add("lazy.simulation.t1SpeedMultiplier", "T1 speed multiplier")
            add("lazy.simulation.t1OutputMultiplier", "T1 output multiplier")
            add("lazy.simulation.t2SpeedMultiplier", "T2 speed multiplier")
            add("lazy.simulation.t2OutputMultiplier", "T2 output multiplier")
            add("lazy.simulation.t3SpeedMultiplier", "T3 speed multiplier")
            add("lazy.simulation.t3OutputMultiplier", "T3 output multiplier")
            add("lazy.simulation.t4SpeedMultiplier", "T4 speed multiplier")
            add("lazy.simulation.t4OutputMultiplier", "T4 output multiplier")
            addItem({ TeleporterRegistries.item.get() }, "Teleporter")
            addItem({ EnergyRegistries.batteryItem.get() }, "Energy Battery")
            add("biome.lazy.void", "Void")
            add("dimension.minecraft.overworld", "Overworld")
            add("dimension.minecraft.the_end", "The End")
            add("dimension.minecraft.the_nether", "The Nether")
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
            add("gui.lazy.io.open", "Open IO settings")
            add("gui.lazy.io.title", "IO settings")
            add("gui.lazy.io.route.passive", "Passive: only allow external extraction")
            add("gui.lazy.io.route.downward", "Output downward")
            add("gui.lazy.io.route.adjacent", "Push to adjacent blocks")
            add("gui.lazy.io.route.network", "Output to network")
            add("gui.lazy.io.network_targets", "Network target")
            add("gui.lazy.io.network_selected", "Target: %s")
            add("gui.lazy.io.network_unselected", "Select a network target")
            add("gui.lazy.io.network_paused", "Network output paused; select a target again")
            add("gui.lazy.io.passive_hint", "Automatic output is disabled")
            add("gui.lazy.io.unavailable", "IO settings unavailable")
            add("gui.lazy.io.provider.beyonddimensions", "Beyond Dimensions main network")
            add("gui.lazy.io.provider.capabilities", "Accepts: %s")
            add("gui.lazy.io.provider.incompatible", "Waiting: this machine's output is not currently supported")
            add("gui.lazy.io.capability.item", "Items")
            add("gui.lazy.io.capability.fluid", "Fluids")
            add("gui.lazy.io.capability.energy", "FE")
            add("message.lazy.io.network.unavailable", "Network integration is unavailable")
            add("message.lazy.io.network.success", "Network output target set: %s")
            add("message.lazy.io.network.no_target", "No primary network target is selected")
            add("message.lazy.io.network.unlinked", "The ME Output Link Card is not linked")
            add("message.lazy.io.network.ambiguous", "Multiple network targets found; hold the intended link card")
            add("message.lazy.io.network.incompatible", "This machine and network have no compatible output capability")
            add("message.lazy.io.network.failed", "Failed to resolve the network target")
            add("gui.lazy.buffer.network_forwarding.toggle", "Toggle network")
            add("gui.lazy.buffer.network_forwarding.enabled", "On")
            add("gui.lazy.buffer.network_forwarding.disabled", "Off")
            add("message.lazy.buffer.network_forwarding", "Dimension network forwarding: %s")
            add("message.lazy.beyond_dimensions.no_primary_network", "No primary dimension network is selected")
            add("message.lazy.beyond_dimensions.unavailable", "Beyond Dimensions integration is unavailable")
            add("gui.lazy.repairer.repair", "Repair item")
            add("tooltip.lazy.buffer.contents", "%s / %s items, %s / %s mB")
            add("tooltip.lazy.buffer.network_forwarding", "Forwards to dimension network #%s")
            add("tooltip.lazy.energy.max_transfer", "Max transfer: %s FE/t")
            add("tooltip.lazy.energy_battery.use", "Sneak-use on an energy-capable block to transfer energy")
            add("tooltip.lazy.energy_source.output_mode", "Use to configure the output mode (passive by default)")
            add("gui.lazy.energy_source.passive", "Passive")
            add("gui.lazy.energy_source.passive.description", "Only allows adjacent devices to pull energy")
            add("gui.lazy.energy_source.active", "Active push")
            add("gui.lazy.energy_source.active.description", "Pushes energy to all six adjacent faces each tick")
            add("gui.lazy.energy_source.network", "Network push")
            add("gui.lazy.energy_source.network.description", "Pushes energy to the selected dimension network")
            add("gui.lazy.item_copier.template.empty", "No item selected")
            add("gui.lazy.item_copier.template.selected", "Copying: %s")
            add(
                "gui.lazy.item_copier.template.description",
                "Click with a carried item to mark it; click with an empty cursor to clear",
            )
            add("gui.lazy.item_copier.interval", "%s ticks")
            add("gui.lazy.item_copier.interval.description", "Click to cycle the push interval")
            add("tooltip.lazy.item_copier.template", "Template: %s")
            add("tooltip.lazy.item_copier.interval", "Push interval: %s ticks")
            add("config.jade.plugin_lazy.buffer", "Buffer status")
            add("config.jade.plugin_lazy.energy_source", "Energy Source status")
            add("config.jade.plugin_lazy.item_copier", "Item Copier status")
            add("config.jade.plugin_lazy.repairer", "Repairer status")
            add("config.jade.plugin_lazy.planter", "Planter status")
            add("jade.lazy.enabled", "Enabled")
            add("jade.lazy.disabled", "Disabled")
            add(
                "jade.lazy.buffer.contents",
                "Items: %s / %s · Fluids: %s / %s mB",
            )
            add("jade.lazy.buffer.network_output", "Network output: %s")
            add("jade.lazy.energy_source.output_mode", "Output mode: %s")
            add("jade.lazy.item_copier.template", "Template: %s")
            add("jade.lazy.repairer.item", "Item: %s")
            add("jade.lazy.repairer.durability", "Durability: %s / %s")
            add("jade.lazy.planter.growth", "Growth: %s%%")
            add("jade.lazy.planter.output_efficiency", "Output efficiency: ×%s")
            add("jade.lazy.planter.output_mode", "Output mode: %s")
            add("jade.lazy.planter.mode.passive", "Passive")
            add("jade.lazy.planter.mode.downward", "Downward output")
            add("jade.lazy.planter.mode.network", "Output to %s")
            add("message.lazy.energy_battery.transfer", "Energy transferred: %s FE")
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
            add("message.lazy.teleporter.dimension_blacklisted", "The teleporter cannot be used in this dimension")
            add("message.lazy.teleporter.dimension_missing", "Teleport failed: target dimension is unavailable")
            add("message.lazy.teleporter.no_safe_destination", "Teleport failed: no safe destination found")
            add("message.lazy.teleporter.no_safe_return", "Teleport failed: the current return point is unsafe")
            add("message.lazy.teleporter.transition_failed", "Teleport failed during dimension transfer")
            add("message.lazy.teleporter.success", "Teleported successfully; cooldown: %s seconds")
            add("screen.lazy.teleporter.returning", "Returning to %s...")
            add("screen.lazy.teleporter.traveling_to_void", "Traveling to the Void...")
            add("curios.identifier.teleporter", "Teleporter")
            add("curios.identifier.me_link_card", "ME Link Card")
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
            LanguageContributions.english().forEach(::add)
        }
    }

    private class ChineseLanguage(
        output: PackOutput,
    ) : LanguageProvider(output, MOD_ID, "zh_cn") {
        override fun addTranslations() {
            addBlock({ MachineCasingRegistries.block.get() }, "机器外壳")
            addBlock({ BufferRegistries.block.get() }, "缓冲器")
            addBlock({ EnergyRegistries.sourceBlock.get() }, "能量源")
            addBlock({ ItemCopierRegistries.block.get() }, "物品复制器")
            addBlock({ RepairerRegistries.block.get() }, "修复器")
            addItem({ TeleporterRegistries.item.get() }, "传送器")
            addItem({ EnergyRegistries.batteryItem.get() }, "能量电池")
            addBlock({ SimulationRegistries.block.get() }, "模拟室")
            addItem({ SimulationRegistries.dataModelItem.get() }, "数据模型")
            addItem({ SimulationRegistries.coreT1.get() }, "T1 模拟核心")
            addItem({ SimulationRegistries.coreT2.get() }, "T2 模拟核心")
            addItem({ SimulationRegistries.coreT3.get() }, "T3 模拟核心")
            addItem({ SimulationRegistries.coreT4.get() }, "T4 模拟核心")
            add("tooltip.lazy.data_model.blank", "未绑定——对生物使用以绑定")
            add("tooltip.lazy.data_model.bound", "已绑定：%s——潜行使用以清除")
            add("tooltip.lazy.simulation_core", "速度 ×%s · 每核心产出 ×%s")
            add("tooltip.lazy.simulation_chamber.contents", "包含已保存的模拟内容")
            add("gui.lazy.simulation_chamber.target", "种子物品或模拟目标")
            add("gui.lazy.simulation_chamber.core", "模拟核心")
            add("gui.lazy.simulation_chamber.progress", "模拟进度")
            add("gui.lazy.simulation_chamber.pending", "无法输出——模拟已暂停")
            add("gui.lazy.simulation_chamber.output_multiplier", "产出倍率：×%s")
            add("gui.lazy.simulation_chamber.speed_multiplier", "速度倍率：×%s")
            add("jei.lazy.item_simulation", "物品模拟")
            add("jei.lazy.entity_simulation", "生物模拟")
            add("jei.lazy.simulation.output_range", "概率：%s%% · 抽取次数：%s–%s")
            add("jei.lazy.simulation.loot_table_output", "战利品表产物；数量与概率动态决定")
            add("config.jade.plugin_lazy.simulation_chamber", "模拟室状态")
            add("jade.lazy.simulation_chamber.progress", "进度：%s%%")
            add("jade.lazy.simulation_chamber.multipliers", "速度 ×%s · 产出 ×%s")
            add("jade.lazy.simulation_chamber.pending", "已暂停：输出积压")
            add("lazy.simulation", "模拟室")
            add("lazy.simulation.desc", "由服务端控制的模拟室设置")
            add("lazy.simulation.defaultDuration", "默认周期时长")
            add("lazy.simulation.maxRollsPerTick", "每刻最大抽取次数")
            add("lazy.simulation.automaticMinerals", "自动矿物配方")
            add("lazy.simulation.automaticMineralDuration", "自动矿物周期时长")
            add("lazy.simulation.automaticMineralModPriority", "自动矿物模组优先级")
            add("lazy.simulation.t1SpeedMultiplier", "T1 速度倍率")
            add("lazy.simulation.t1OutputMultiplier", "T1 产出倍率")
            add("lazy.simulation.t2SpeedMultiplier", "T2 速度倍率")
            add("lazy.simulation.t2OutputMultiplier", "T2 产出倍率")
            add("lazy.simulation.t3SpeedMultiplier", "T3 速度倍率")
            add("lazy.simulation.t3OutputMultiplier", "T3 产出倍率")
            add("lazy.simulation.t4SpeedMultiplier", "T4 速度倍率")
            add("lazy.simulation.t4OutputMultiplier", "T4 产出倍率")
            add("biome.lazy.void", "虚空")
            add("dimension.minecraft.overworld", "主世界")
            add("dimension.minecraft.the_end", "末地")
            add("dimension.minecraft.the_nether", "下界")
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
            add("gui.lazy.io.open", "打开 IO 设置")
            add("gui.lazy.io.title", "IO 设置")
            add("gui.lazy.io.route.passive", "被动：仅允许外部抽取")
            add("gui.lazy.io.route.downward", "向下输出")
            add("gui.lazy.io.route.adjacent", "向相邻方块主动推送")
            add("gui.lazy.io.route.network", "输出到网络")
            add("gui.lazy.io.network_targets", "网络目标")
            add("gui.lazy.io.network_selected", "目标：%s")
            add("gui.lazy.io.network_unselected", "请选择网络目标")
            add("gui.lazy.io.network_paused", "网络输出已暂停；请重新选择目标")
            add("gui.lazy.io.passive_hint", "已关闭自动输出")
            add("gui.lazy.io.unavailable", "IO 设置不可用")
            add("gui.lazy.io.provider.beyonddimensions", "超越维度主网络")
            add("gui.lazy.io.provider.capabilities", "可接收：%s")
            add("gui.lazy.io.provider.incompatible", "等待中：当前不支持该机器的输出能力")
            add("gui.lazy.io.capability.item", "物品")
            add("gui.lazy.io.capability.fluid", "流体")
            add("gui.lazy.io.capability.energy", "FE")
            add("message.lazy.io.network.unavailable", "网络集成当前不可用")
            add("message.lazy.io.network.success", "网络输出目标已设置：%s")
            add("message.lazy.io.network.no_target", "尚未选择主网络目标")
            add("message.lazy.io.network.unlinked", "ME 输出链接卡尚未链接")
            add("message.lazy.io.network.ambiguous", "发现多个网络目标；请手持所需的链接卡")
            add("message.lazy.io.network.incompatible", "该机器与网络没有兼容的输出能力")
            add("message.lazy.io.network.failed", "解析网络目标失败")
            add("gui.lazy.buffer.network_forwarding.toggle", "切换网络直送")
            add("gui.lazy.buffer.network_forwarding.enabled", "开")
            add("gui.lazy.buffer.network_forwarding.disabled", "关")
            add("message.lazy.buffer.network_forwarding", "维度网络直送：%s")
            add("message.lazy.beyond_dimensions.no_primary_network", "尚未选择主维度网络")
            add("message.lazy.beyond_dimensions.unavailable", "超越维度兼容当前不可用")
            add("gui.lazy.repairer.repair", "修复物品")
            add("tooltip.lazy.buffer.contents", "物品 %s / %s，流体 %s / %s mB")
            add("tooltip.lazy.buffer.network_forwarding", "直送至维度网络 #%s")
            add("tooltip.lazy.energy.max_transfer", "最大传输：%s FE/t")
            add("tooltip.lazy.energy_battery.use", "潜行对支持能量的方块使用以传输能量")
            add("tooltip.lazy.energy_source.output_mode", "使用以配置输出模式（默认被动）")
            add("gui.lazy.energy_source.passive", "被动")
            add("gui.lazy.energy_source.passive.description", "仅允许相邻设备主动抽取能量")
            add("gui.lazy.energy_source.active", "主动推送")
            add("gui.lazy.energy_source.active.description", "每刻向六个相邻面推送能量")
            add("gui.lazy.energy_source.network", "网络推送")
            add("gui.lazy.energy_source.network.description", "向选中的维度网络推送能量")
            add("gui.lazy.item_copier.template.empty", "未标记物品")
            add("gui.lazy.item_copier.template.selected", "正在复制：%s")
            add("gui.lazy.item_copier.template.description", "鼠标携带物品时点击进行标记；空鼠标点击清空")
            add("gui.lazy.item_copier.interval", "%s 刻")
            add("gui.lazy.item_copier.interval.description", "点击循环切换推送间隔")
            add("tooltip.lazy.item_copier.template", "模板：%s")
            add("tooltip.lazy.item_copier.interval", "推送间隔：%s 刻")
            add("config.jade.plugin_lazy.buffer", "缓冲器状态")
            add("config.jade.plugin_lazy.energy_source", "能量源状态")
            add("config.jade.plugin_lazy.item_copier", "物品复制器状态")
            add("config.jade.plugin_lazy.repairer", "修复器状态")
            add("config.jade.plugin_lazy.planter", "种植机状态")
            add("jade.lazy.enabled", "开启")
            add("jade.lazy.disabled", "关闭")
            add(
                "jade.lazy.buffer.contents",
                "物品：%s / %s · 流体：%s / %s mB",
            )
            add("jade.lazy.buffer.network_output", "网络输出：%s")
            add("jade.lazy.energy_source.output_mode", "输出模式：%s")
            add("jade.lazy.item_copier.template", "模板：%s")
            add("jade.lazy.repairer.item", "物品：%s")
            add("jade.lazy.repairer.durability", "耐久：%s / %s")
            add("jade.lazy.planter.growth", "生长进度：%s%%")
            add("jade.lazy.planter.output_efficiency", "产出效率：×%s")
            add("jade.lazy.planter.output_mode", "输出模式：%s")
            add("jade.lazy.planter.mode.passive", "被动")
            add("jade.lazy.planter.mode.downward", "向下输出")
            add("jade.lazy.planter.mode.network", "输出到%s")
            add("message.lazy.energy_battery.transfer", "已传输能量：%s FE")
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
            add("message.lazy.teleporter.dimension_blacklisted", "当前维度禁止使用传送器")
            add("message.lazy.teleporter.dimension_missing", "传送失败：目标维度不可用")
            add("message.lazy.teleporter.no_safe_destination", "传送失败：未找到安全落点")
            add("message.lazy.teleporter.no_safe_return", "传送失败：当前位置不能作为安全返回点")
            add("message.lazy.teleporter.transition_failed", "跨维度传送失败")
            add("message.lazy.teleporter.success", "传送成功；冷却 %s 秒")
            add("screen.lazy.teleporter.returning", "正在返回%s……")
            add("screen.lazy.teleporter.traveling_to_void", "正在前往虚空……")
            add("curios.identifier.teleporter", "传送器")
            add("curios.identifier.me_link_card", "ME链接卡")
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
            LanguageContributions.chinese().forEach(::add)
        }
    }
}
