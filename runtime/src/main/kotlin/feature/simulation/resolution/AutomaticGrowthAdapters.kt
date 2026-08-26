package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import rhx.lazy.core.lazyId

internal object TreeSimulationAdapter : AutomaticSimulationAdapter {
    val SOURCE: ResourceLocation = lazyId("tree")

    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (!stack.`is`(ItemTags.SAPLINGS)) return null
        val inputId = BuiltInRegistries.ITEM.getKey(stack.item)
        val pair = automaticTreePair(inputId) ?: return null
        val log = BuiltInRegistries.BLOCK.getOptional(pair.log).orElse(null) ?: return null
        val leaves = BuiltInRegistries.BLOCK.getOptional(pair.leaves).orElse(null) ?: return null
        return AutomaticSimulationCandidate(
            SOURCE,
            automaticId(SOURCE, inputId.namespace, pair.base),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            blockLootOutputs =
                listOf(
                    blockLoot(level, log.defaultBlockState(), minRolls = 1, maxRolls = 4),
                    blockLoot(level, leaves.defaultBlockState(), ItemStack(Items.SHEARS), minRolls = 1, maxRolls = 3),
                    blockLoot(level, leaves.defaultBlockState(), minRolls = 1, maxRolls = 3),
                ),
        )
    }

    private const val PRIORITY = 200
}

internal object CropSimulationAdapter : AutomaticSimulationAdapter {
    val SOURCE: ResourceLocation = lazyId("crop")

    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (stack.`is`(Items.MELON_SEEDS)) return stem(level, stack, Items.MELON, Items.MELON_SEEDS, includeFruit = true)
        if (stack.`is`(Items.PUMPKIN_SEEDS)) return stem(level, stack, Items.PUMPKIN, Items.PUMPKIN_SEEDS, includeFruit = false)
        val target = AutomaticGrowthIndex.resolve(level, stack.item) ?: return null
        return AutomaticSimulationCandidate(
            SOURCE,
            inputId(SOURCE, stack),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            claimsInput = true,
            blockLootOutputs =
                listOf(
                    SimulationBlockLootOutput(
                        target.state,
                        target.displayItems.map(ItemStack::copy),
                    ),
                ),
        )
    }

    private fun stem(
        level: Level,
        stack: ItemStack,
        fruit: Item,
        seed: Item,
        includeFruit: Boolean,
    ): AutomaticSimulationCandidate {
        val fruitBlock = (fruit as BlockItem).block
        return AutomaticSimulationCandidate(
            SOURCE,
            inputId(SOURCE, stack),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            itemOutputs =
                buildList {
                    if (includeFruit) add(SimulationItemOutput(ItemStack(fruit)))
                    add(SimulationItemOutput(ItemStack(seed), chance = 0.05f))
                },
            blockLootOutputs = listOf(blockLoot(level, fruitBlock.defaultBlockState())),
        )
    }

    private const val PRIORITY = 200
}

internal object PlantSimulationAdapter : AutomaticSimulationAdapter {
    val SOURCE: ResourceLocation = lazyId("plant")

    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (!stack.`is`(SimulationTags.plantTargets)) return null
        val state = automaticPlantState(stack) ?: return null
        return AutomaticSimulationCandidate(
            SOURCE,
            inputId(SOURCE, stack),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            blockLootOutputs = listOf(blockLoot(level, state, ItemStack(Items.SHEARS))),
        )
    }

    private const val PRIORITY = 200
}

internal data class AutomaticTreePair(
    val base: String,
    val log: ResourceLocation,
    val leaves: ResourceLocation,
)

internal fun automaticTreePair(input: ResourceLocation): AutomaticTreePair? {
    val base =
        when {
            input.path.endsWith("_sapling") -> input.path.removeSuffix("_sapling")
            input.path == "mangrove_propagule" -> "mangrove"
            else -> return null
        }
    return AutomaticTreePair(
        base,
        ResourceLocation.fromNamespaceAndPath(input.namespace, "${base}_log"),
        ResourceLocation.fromNamespaceAndPath(input.namespace, "${base}_leaves"),
    )
}

internal fun matureCropState(stack: ItemStack): BlockState? {
    val crop =
        Item.BY_BLOCK
            .asSequence()
            .firstOrNull { (block, item) -> item === stack.item && block is CropBlock }
            ?.key as? CropBlock ?: return null
    return crop.getStateForAge(crop.maxAge)
}

internal fun automaticPlantState(stack: ItemStack): BlockState? {
    val block = (stack.item as? BlockItem)?.block ?: return null
    if (block is CropBlock) return null
    val state = block.defaultBlockState()
    if (state.isAir) return null
    return if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
        state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
    } else {
        state
    }
}

/** Recipe id for sources keyed by their input item, e.g. `lazy:automatic/plant/minecraft/poppy`. */
internal fun inputId(
    source: ResourceLocation,
    stack: ItemStack,
): ResourceLocation {
    val input = BuiltInRegistries.ITEM.getKey(stack.item)
    return automaticId(source, input.namespace, input.path)
}
