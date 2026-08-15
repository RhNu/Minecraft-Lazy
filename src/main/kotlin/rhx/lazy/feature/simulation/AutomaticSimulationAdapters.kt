package rhx.lazy.feature.simulation

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import rhx.lazy.Lazy
import rhx.lazy.core.lazyId

internal data class AutomaticSimulationCandidate(
    val source: ResourceLocation,
    val id: ResourceLocation,
    val duration: Int,
    val priority: Int,
    val claimsInput: Boolean = false,
    val itemOutputs: List<SimulationItemOutput> = emptyList(),
    val fluidOutputs: List<SimulationFluidOutput> = emptyList(),
    val blockLootOutputs: List<SimulationBlockLootOutput> = emptyList(),
)

internal fun interface AutomaticSimulationAdapter {
    fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate?

    fun settingsFingerprint(): Any? = null
}

internal object AutomaticSimulationAdapters {
    private val external = linkedMapOf<ResourceLocation, AutomaticSimulationAdapter>()
    private val builtIns =
        listOf(TreeSimulationAdapter, CropSimulationAdapter, PlantSimulationAdapter, MineralSimulationAdapter)

    @Synchronized
    fun register(
        source: ResourceLocation,
        adapter: AutomaticSimulationAdapter,
    ) {
        require(source.namespace != "lazy" || source.path !in setOf("tree", "crop", "plant", "mineral")) {
            "Automatic simulation source $source is reserved"
        }
        require(external.putIfAbsent(source, adapter) == null) { "Duplicate automatic simulation source $source" }
        SimulationRecipeResolver.invalidate()
    }

    fun resolve(
        level: Level,
        stack: ItemStack,
    ): List<AutomaticSimulationCandidate> =
        (builtIns + synchronized(this) { external.values.toList() })
            .mapNotNull { adapter -> adapter.resolve(level, stack) }
            .filterNot { candidate ->
                stack.`is`(SimulationTags.automaticSimulationBlacklist) ||
                    SimulationTags.automaticBlacklist(candidate.source)?.let(stack::`is`) == true
            }.let { candidates ->
                val claimed = candidates.filter(AutomaticSimulationCandidate::claimsInput)
                if (claimed.isEmpty()) candidates else listOf(claimed.maxWith(candidateComparator))
            }.sortedWith(candidateComparator)

    fun settingsFingerprint(): List<Pair<ResourceLocation, Any?>> =
        synchronized(this) { external.map { (source, adapter) -> source to adapter.settingsFingerprint() } }

    private val candidateComparator =
        compareByDescending<AutomaticSimulationCandidate>(AutomaticSimulationCandidate::priority)
            .thenBy { it.source.toString() }
            .thenBy { it.id.toString() }
}

private object TreeSimulationAdapter : AutomaticSimulationAdapter {
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
            lazyId("automatic/tree/${inputId.namespace}/${pair.base}"),
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

    private val SOURCE = lazyId("tree")
    private const val PRIORITY = 200
}

private object CropSimulationAdapter : AutomaticSimulationAdapter {
    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (stack.`is`(Items.MELON_SEEDS)) return stem(level, stack, Items.MELON, Items.MELON_SEEDS, includeFruit = true)
        if (stack.`is`(Items.PUMPKIN_SEEDS)) return stem(level, stack, Items.PUMPKIN, Items.PUMPKIN_SEEDS, includeFruit = false)
        val state = matureCropState(stack) ?: return null
        return AutomaticSimulationCandidate(
            SOURCE,
            automaticId("crop", stack),
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
            automaticId("crop", stack),
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

    private val SOURCE = lazyId("crop")
    private const val PRIORITY = 200
}

private object PlantSimulationAdapter : AutomaticSimulationAdapter {
    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (!stack.`is`(SimulationTags.automaticPlantTargets)) return null
        val state = automaticPlantState(stack) ?: return null
        return AutomaticSimulationCandidate(
            SOURCE,
            automaticId("plant", stack),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            blockLootOutputs = listOf(blockLoot(level, state, ItemStack(Items.SHEARS))),
        )
    }

    private val SOURCE = lazyId("plant")
    private const val PRIORITY = 200
}

private object MineralSimulationAdapter : AutomaticSimulationAdapter {
    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (!SimulationConfigs.settings.automaticMinerals.get()) return null
        val tags = stack.tags.toList()
        val ingots = tags.mapNotNull { mineralMaterial(it, "ingots/") }.distinct()
        val gems = tags.mapNotNull { mineralMaterial(it, "gems/") }.distinct()
        val dusts = tags.mapNotNull { mineralMaterial(it, "dusts/") }.distinct()
        val matchedMaterials = ingots.size + gems.size + dusts.size
        if (matchedMaterials == 0) {
            return if (stack.`is`(Items.COAL)) vanillaCoalCandidate() else null
        }
        if (matchedMaterials != 1) return null

        val kind: String
        val material: String
        val outputTag: TagKey<Item>
        if (gems.size == 1) {
            kind = "gem"
            material = gems.single()
            outputTag = tag("gems/$material")
        } else if (dusts.size == 1) {
            kind = "dust"
            material = dusts.single()
            outputTag = tag("dusts/$material")
        } else {
            kind = "ingot"
            material = ingots.single()
            outputTag = tag("raw_materials/$material")
        }
        val output = preferredItem(outputTag) ?: return null
        return AutomaticSimulationCandidate(
            SOURCE,
            lazyId("automatic/$kind/$material"),
            SimulationConfigs.settings.automaticMineralDuration.get(),
            PRIORITY,
            itemOutputs = listOf(SimulationItemOutput(ItemStack(output))),
        )
    }

    private fun vanillaCoalCandidate() =
        AutomaticSimulationCandidate(
            SOURCE,
            lazyId("automatic/coal"),
            SimulationConfigs.settings.automaticMineralDuration.get(),
            PRIORITY,
            itemOutputs = listOf(SimulationItemOutput(ItemStack(Items.COAL))),
        )

    private fun preferredItem(tag: TagKey<Item>): Item? {
        val priorities = SimulationConfigs.settings.automaticMineralModPriority.get()
        val idComparator = mineralCandidateIdComparator(priorities)
        return BuiltInRegistries.ITEM
            .getTag(tag)
            .orElse(null)
            ?.asSequence()
            ?.map { it.value() }
            ?.filterNot { it === Items.AIR }
            ?.minWithOrNull { first, second ->
                idComparator.compare(BuiltInRegistries.ITEM.getKey(first), BuiltInRegistries.ITEM.getKey(second))
            }
    }

    private fun tag(path: String) = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("c", path))

    private val SOURCE = lazyId("mineral")
    private const val PRIORITY = 100
}

internal fun mineralMaterial(
    tag: TagKey<Item>,
    prefix: String,
): String? {
    val id = tag.location
    return if (id.namespace == "c" && id.path.startsWith(prefix) && id.path.length > prefix.length) {
        id.path.removePrefix(prefix)
    } else {
        null
    }
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

internal fun mineralCandidateIdComparator(priorities: List<String>): Comparator<ResourceLocation> =
    compareBy<ResourceLocation>(
        { id -> priorities.indexOf(id.namespace).let { if (it < 0) Int.MAX_VALUE else it } },
        ResourceLocation::getNamespace,
        ResourceLocation::toString,
    )

private object SimulationLootDisplays {
    fun items(
        level: ServerLevel,
        state: BlockState,
    ): List<ItemStack> {
        val table = level.server.reloadableRegistries().getLootTable(state.block.lootTable)
        val context = level.registryAccess().createSerializationContext(JsonOps.INSTANCE)
        val json = LootTableJson(table, context).encode() ?: return fallback(state)
        val items = linkedMapOf<Item, ItemStack>()
        collect(json, items)
        return items.values.map(ItemStack::copy).ifEmpty { fallback(state) }
    }

    private fun collect(
        element: JsonElement,
        items: MutableMap<Item, ItemStack>,
    ) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collect(it, items) }
            element.isJsonObject -> {
                val objectValue = element.asJsonObject
                when (objectValue.string("type")) {
                    "minecraft:item" -> objectValue.string("name")?.let { addItem(it, items) }
                    "minecraft:tag" -> objectValue.string("name")?.let { addTag(it, items) }
                }
                objectValue.entrySet().forEach { (_, child) -> collect(child, items) }
            }
        }
    }

    private fun addItem(
        rawId: String,
        items: MutableMap<Item, ItemStack>,
    ) {
        val id = ResourceLocation.tryParse(rawId) ?: return
        BuiltInRegistries.ITEM
            .getOptional(id)
            .orElse(null)
            ?.takeUnless { it === Items.AIR }
            ?.let { items.putIfAbsent(it, ItemStack(it)) }
    }

    private fun addTag(
        rawId: String,
        items: MutableMap<Item, ItemStack>,
    ) {
        val id = ResourceLocation.tryParse(rawId) ?: return
        val tag = TagKey.create(BuiltInRegistries.ITEM.key(), id)
        BuiltInRegistries.ITEM.getTag(tag).orElse(null)?.forEach { holder ->
            holder.value().takeUnless { it === Items.AIR }?.let { items.putIfAbsent(it, ItemStack(it)) }
        }
    }

    private fun fallback(state: BlockState): List<ItemStack> =
        state.block
            .asItem()
            .takeUnless { it === Items.AIR }
            ?.let { listOf(ItemStack(it)) }
            .orEmpty()

    private fun JsonObject.string(name: String): String? = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asString

    private class LootTableJson(
        private val table: net.minecraft.world.level.storage.loot.LootTable,
        private val ops: com.mojang.serialization.DynamicOps<JsonElement>,
    ) {
        fun encode(): JsonElement? =
            net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC
                .encodeStart(ops, table)
                .resultOrPartial { message -> Lazy.logger.warn("Failed to inspect simulation loot table: {}", message) }
                .orElse(null)
    }
}

private fun automaticId(
    kind: String,
    stack: ItemStack,
): ResourceLocation {
    val input = BuiltInRegistries.ITEM.getKey(stack.item)
    return lazyId("automatic/$kind/${input.namespace}/${input.path}")
}

private fun blockLoot(
    level: Level,
    state: BlockState,
    tool: ItemStack = ItemStack.EMPTY,
) = SimulationBlockLootOutput(
    state,
    (level as? ServerLevel)?.let { SimulationLootDisplays.items(it, state) }.orEmpty(),
    tool,
)
