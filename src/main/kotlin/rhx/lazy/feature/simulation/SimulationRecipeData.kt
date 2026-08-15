package rhx.lazy.feature.simulation

import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.common.NeoForgeMod
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.lazyId

internal object SimulationRecipeData {
    fun build(output: RecipeOutput) {
        item("bamboo", Items.BAMBOO, output, itemOutput(Items.BAMBOO, max = 3))
        item("brown_mushroom", Items.BROWN_MUSHROOM, output, itemOutput(Items.BROWN_MUSHROOM))
        item("cactus", Items.CACTUS, output, itemOutput(Items.CACTUS))
        item("chorus", Items.CHORUS_FLOWER, output, itemOutput(Items.CHORUS_FRUIT, max = 3))
        item("cocoa", Items.COCOA_BEANS, output, itemOutput(Items.COCOA_BEANS, min = 2, max = 3))
        item("glow_berries", Items.GLOW_BERRIES, output, itemOutput(Items.GLOW_BERRIES))
        item("kelp", Items.KELP, output, itemOutput(Items.KELP))
        item("nether_wart", Items.NETHER_WART, output, itemOutput(Items.NETHER_WART, min = 2, max = 4))
        // c:raw_materials/netherite 不存在，ingot 规则查不到产物，补一条显式配方接上这条链
        item("netherite_ingot", Items.NETHERITE_INGOT, output, itemOutput(Items.NETHERITE_SCRAP))
        item("red_mushroom", Items.RED_MUSHROOM, output, itemOutput(Items.RED_MUSHROOM))
        item("sugar_cane", Items.SUGAR_CANE, output, itemOutput(Items.SUGAR_CANE))
        item("sweet_berries", Items.SWEET_BERRIES, output, itemOutput(Items.SWEET_BERRIES, min = 2, max = 3))

        entity("blaze", output, Items.BLAZE_ROD)
        entity("bogged", output, Items.BONE, Items.ARROW)
        entity("breeze", output, Items.BREEZE_ROD)
        entity("cave_spider", output, Items.STRING, Items.SPIDER_EYE)
        entity("chicken", output, Items.FEATHER, Items.CHICKEN)
        entity("cod", output, Items.COD, Items.BONE_MEAL)
        entity(
            "cow",
            output,
            Items.LEATHER,
            Items.BEEF,
            fluidOutputs = listOf(SimulationFluidOutput(FluidStack(NeoForgeMod.MILK.get(), 1000), 0.3f)),
            displayFluids = listOf(FluidStack(NeoForgeMod.MILK.get(), 1000)),
        )
        entity("creeper", output, Items.GUNPOWDER)
        entity("dolphin", output, Items.COD)
        entity("donkey", output, Items.LEATHER)
        entity("drowned", output, Items.ROTTEN_FLESH, Items.COPPER_INGOT)
        entity("elder_guardian", output, Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS, Items.WET_SPONGE)
        entity("enderman", output, Items.ENDER_PEARL)
        entity("evoker", output, Items.EMERALD, Items.TOTEM_OF_UNDYING)
        entity("ghast", output, Items.GHAST_TEAR, Items.GUNPOWDER)
        entity("glow_squid", output, Items.GLOW_INK_SAC)
        entity("guardian", output, Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS)
        entity("hoglin", output, Items.PORKCHOP, Items.LEATHER)
        entity("horse", output, Items.LEATHER)
        entity("husk", output, Items.ROTTEN_FLESH)
        entity("iron_golem", output, Items.IRON_INGOT, Items.POPPY)
        entity("llama", output, Items.LEATHER)
        entity("magma_cube", output, Items.MAGMA_CREAM)
        entity("mooshroom", output, Items.LEATHER, Items.BEEF)
        entity("mule", output, Items.LEATHER)
        entity("panda", output, Items.BAMBOO)
        entity("parrot", output, Items.FEATHER)
        entity("phantom", output, Items.PHANTOM_MEMBRANE)
        entity("pig", output, Items.PORKCHOP)
        entity("pillager", output, Items.CROSSBOW)
        entity("polar_bear", output, Items.COD, Items.SALMON)
        entity("pufferfish", output, Items.PUFFERFISH, Items.BONE_MEAL)
        entity("rabbit", output, Items.RABBIT, Items.RABBIT_HIDE, Items.RABBIT_FOOT)
        entity("ravager", output, Items.SADDLE)
        entity("salmon", output, Items.SALMON, Items.BONE_MEAL)
        entity("sheep", output, Items.WHITE_WOOL, Items.MUTTON)
        entity("shulker", output, Items.SHULKER_SHELL)
        entity("skeleton", output, Items.BONE, Items.ARROW)
        entity("slime", output, Items.SLIME_BALL)
        entity("snow_golem", output, Items.SNOWBALL)
        entity("spider", output, Items.STRING, Items.SPIDER_EYE)
        entity("squid", output, Items.INK_SAC)
        entity("stray", output, Items.BONE, Items.ARROW)
        entity("strider", output, Items.STRING)
        entity("tropical_fish", output, Items.TROPICAL_FISH, Items.BONE_MEAL)
        entity("vindicator", output, Items.EMERALD)
        entity("warden", output, Items.SCULK_CATALYST)
        entity(
            "witch",
            output,
            Items.REDSTONE,
            Items.GLOWSTONE_DUST,
            Items.SUGAR,
            Items.SPIDER_EYE,
            Items.GLASS_BOTTLE,
            Items.STICK,
        )
        entity("wither", output, Items.NETHER_STAR)
        entity("wither_skeleton", output, Items.COAL, Items.BONE, Items.WITHER_SKELETON_SKULL)
        entity("zoglin", output, Items.ROTTEN_FLESH)
        entity("zombie", output, Items.ROTTEN_FLESH)
        entity("zombie_villager", output, Items.ROTTEN_FLESH)
        entity("zombified_piglin", output, Items.ROTTEN_FLESH, Items.GOLD_NUGGET, Items.GOLD_INGOT)
    }

    private fun item(
        name: String,
        input: Item,
        output: RecipeOutput,
        vararg outputs: SimulationItemOutput,
    ) {
        output.accept(
            lazyId("simulation/$name"),
            ItemSimulationRecipe(Ingredient.of(input), itemOutputs = outputs.toList()),
            null,
        )
    }

    private fun itemOutput(
        item: Item,
        chance: Float = 1f,
        min: Int = 1,
        max: Int = min,
    ) = SimulationItemOutput(ItemStack(item), chance, min, max)

    private fun entity(
        name: String,
        output: RecipeOutput,
        vararg displayItems: Item,
        fluidOutputs: List<SimulationFluidOutput> = emptyList(),
        displayFluids: List<FluidStack> = emptyList(),
    ) {
        output.accept(
            lazyId("simulation/entity/$name"),
            EntitySimulationRecipe(
                ResourceLocation.withDefaultNamespace(name),
                fluidOutputs = fluidOutputs,
                displayItemOutputs = displayItems.map(::ItemStack),
                displayFluidOutputs = displayFluids,
            ),
            null,
        )
    }
}
