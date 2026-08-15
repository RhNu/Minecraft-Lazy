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
        val log = item(pair.log) ?: return null
        val leaves = item(pair.leaves) ?: return null
        val outputs =
            buildList {
                add(SimulationItemOutput(ItemStack(log), minRolls = 1, maxRolls = 4))
                add(SimulationItemOutput(stack.copyWithCount(1), chance = 0.05f))
                add(SimulationItemOutput(ItemStack(leaves), minRolls = 1, maxRolls = 3))
                vanillaTreeExtras(inputId).let(::addAll)
            }
        return AutomaticSimulationCandidate(
            SOURCE,
            automaticId(SOURCE, inputId.namespace, pair.base),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            itemOutputs = outputs,
        )
    }

    private fun vanillaTreeExtras(input: ResourceLocation): List<SimulationItemOutput> {
        if (input.namespace != "minecraft") return emptyList()
        return when (input.path) {
            "oak_sapling", "dark_oak_sapling" -> listOf(SimulationItemOutput(ItemStack(Items.APPLE), 0.05f))
            "jungle_sapling" -> listOf(SimulationItemOutput(ItemStack(Items.COCOA_BEANS), 0.05f))
            "mangrove_propagule" ->
                listOf(
                    SimulationItemOutput(ItemStack(Items.MANGROVE_ROOTS), 0.05f),
                    SimulationItemOutput(ItemStack(Items.MUDDY_MANGROVE_ROOTS), 0.01f),
                )
            else -> emptyList()
        }
    }

    private fun item(id: ResourceLocation): Item? =
        BuiltInRegistries.ITEM
            .getOptional(id)
            .orElse(null)
            ?.takeUnless { it === Items.AIR }

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
        val state = matureCropState(stack) ?: return null
        return AutomaticSimulationCandidate(
            SOURCE,
            inputId(SOURCE, stack),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            blockLootOutputs = listOf(blockLoot(level, state)),
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
    val crop = ((stack.item as? BlockItem)?.block as? CropBlock) ?: return null
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
