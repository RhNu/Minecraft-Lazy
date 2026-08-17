package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.neoforged.neoforge.common.Tags
import rhx.lazy.core.lazyId
import java.util.WeakHashMap

internal enum class AutomaticGrowthEvidence {
    BLOCK_ITEM_MAPPING,
    CROP_CLASS,
    CROP_TAG,
    SEED_TAG,
    VILLAGER_SEED_TAG,
    BONEMEALABLE,
}

internal data class AutomaticGrowthCandidate(
    val input: Item,
    val block: Block,
    val evidence: Set<AutomaticGrowthEvidence>,
)

internal data class AutomaticGrowthTarget(
    val state: BlockState,
    val displayItems: List<ItemStack>,
    val evidence: Set<AutomaticGrowthEvidence>,
)

internal fun interface AutomaticGrowthCandidateSource {
    fun collect(sink: AutomaticGrowthCandidateSink)
}

/**
 * Merges independent discovery signals into one `(input item, placed block)` graph. Sources only
 * provide evidence; state selection and loot validation stay centralized so adding a new source
 * cannot silently invent a second recipe for the same pair.
 */
internal object AutomaticGrowthCandidateSources {
    private val sources = linkedMapOf<ResourceLocation, AutomaticGrowthCandidateSource>()

    init {
        registerBuiltIn(lazyId("growth/block_item_mapping"), BlockItemMappingSource)
        registerBuiltIn(lazyId("growth/crop_structure"), CropStructureSource)
        registerBuiltIn(lazyId("growth/seed_tag"), SeedTagSource)
    }

    @Synchronized
    fun register(
        id: ResourceLocation,
        source: AutomaticGrowthCandidateSource,
    ) {
        require(sources.putIfAbsent(id, source) == null) { "Duplicate automatic growth candidate source $id" }
        SimulationRecipeResolver.invalidate()
    }

    fun collect(): Map<Item, List<AutomaticGrowthCandidate>> =
        AutomaticGrowthCandidateSink()
            .also { sink -> synchronized(this) { sources.values.toList() }.forEach { it.collect(sink) } }
            .finish()

    private fun registerBuiltIn(
        id: ResourceLocation,
        source: AutomaticGrowthCandidateSource,
    ) {
        check(sources.put(id, source) == null) { "Duplicate built-in automatic growth candidate source $id" }
    }
}

internal class AutomaticGrowthCandidateSink {
    private val pairs = linkedMapOf<Pair<Item, Block>, MutableSet<AutomaticGrowthEvidence>>()
    private val inputEvidence = linkedMapOf<Item, MutableSet<AutomaticGrowthEvidence>>()

    fun add(
        input: Item,
        block: Block,
        vararg evidence: AutomaticGrowthEvidence,
    ) {
        if (input === Items.AIR || block.defaultBlockState().isAir) return
        pairs.getOrPut(input to block, ::linkedSetOf).addAll(evidence)
    }

    fun addInputEvidence(
        input: Item,
        evidence: AutomaticGrowthEvidence,
    ) {
        if (input !== Items.AIR) inputEvidence.getOrPut(input, ::linkedSetOf) += evidence
    }

    fun finish(): Map<Item, List<AutomaticGrowthCandidate>> =
        pairs
            .mapNotNull { (pair, pairEvidence) ->
                val (input, block) = pair
                val combined = pairEvidence + inputEvidence[input].orEmpty()
                if (!supportsMatureState(block, combined)) return@mapNotNull null
                AutomaticGrowthCandidate(input, block, combined)
            }.groupBy(AutomaticGrowthCandidate::input)
            .mapValues { (_, candidates) -> candidates.sortedBy { BuiltInRegistries.BLOCK.getKey(it.block).toString() } }
}

internal object AutomaticGrowthIndex {
    private val indices = WeakHashMap<RecipeManager, Index>()

    fun resolve(
        level: Level,
        input: Item,
    ): AutomaticGrowthTarget? {
        val index = indexFor(level)
        if (input in index.resolved) return index.resolved[input]
        return select(level, input, index.candidates[input].orEmpty()).also { index.resolved[input] = it }
    }

    @Synchronized
    fun invalidate() {
        indices.clear()
    }

    @Synchronized
    private fun indexFor(level: Level): Index =
        indices.getOrPut(level.recipeManager) {
            Index(AutomaticGrowthCandidateSources.collect())
        }

    private fun select(
        level: Level,
        input: Item,
        candidates: List<AutomaticGrowthCandidate>,
    ): AutomaticGrowthTarget? =
        selectAutomaticGrowthTarget(
            input,
            candidates
                .mapNotNull { candidate ->
                    val state = matureState(candidate) ?: return@mapNotNull null
                    AutomaticGrowthTarget(
                        state,
                        SimulationLootDisplays.items(level, state),
                        candidate.evidence,
                    )
                },
        )

    private class Index(
        val candidates: Map<Item, List<AutomaticGrowthCandidate>>,
    ) {
        val resolved = hashMapOf<Item, AutomaticGrowthTarget?>()
    }
}

internal fun selectAutomaticGrowthTarget(
    input: Item,
    targets: List<AutomaticGrowthTarget>,
): AutomaticGrowthTarget? =
    targets
        .sortedWith(
            compareByDescending<AutomaticGrowthTarget> { target -> target.displayItems.any { it.item !== input } }
                .thenByDescending { target -> target.displayItems.any { it.item === input } }
                .thenByDescending { AutomaticGrowthEvidence.CROP_CLASS in it.evidence }
                .thenByDescending { AutomaticGrowthEvidence.CROP_TAG in it.evidence }
                .thenBy { BuiltInRegistries.BLOCK.getKey(it.state.block).toString() },
        ).firstOrNull()

private object BlockItemMappingSource : AutomaticGrowthCandidateSource {
    override fun collect(sink: AutomaticGrowthCandidateSink) {
        Item.BY_BLOCK.forEach { (block, item) ->
            sink.add(item, block, AutomaticGrowthEvidence.BLOCK_ITEM_MAPPING)
        }
    }
}

private object CropStructureSource : AutomaticGrowthCandidateSource {
    override fun collect(sink: AutomaticGrowthCandidateSink) {
        BuiltInRegistries.BLOCK.forEach { block ->
            val evidence =
                buildList {
                    if (block is CropBlock) add(AutomaticGrowthEvidence.CROP_CLASS)
                    if (block.defaultBlockState().`is`(BlockTags.CROPS)) add(AutomaticGrowthEvidence.CROP_TAG)
                    if (block is BonemealableBlock) add(AutomaticGrowthEvidence.BONEMEALABLE)
                }
            if (evidence.isNotEmpty()) sink.add(block.asItem(), block, *evidence.toTypedArray())
        }
    }
}

private object SeedTagSource : AutomaticGrowthCandidateSource {
    override fun collect(sink: AutomaticGrowthCandidateSink) {
        BuiltInRegistries.ITEM.getTag(Tags.Items.SEEDS).orElse(null)?.forEach { holder ->
            sink.addInputEvidence(holder.value(), AutomaticGrowthEvidence.SEED_TAG)
        }
        BuiltInRegistries.ITEM.getTag(ItemTags.VILLAGER_PLANTABLE_SEEDS).orElse(null)?.forEach { holder ->
            sink.addInputEvidence(holder.value(), AutomaticGrowthEvidence.VILLAGER_SEED_TAG)
        }
    }
}

private fun supportsMatureState(
    block: Block,
    evidence: Set<AutomaticGrowthEvidence>,
): Boolean {
    if (block is CropBlock) return true
    val hasAge = ageProperty(block) != null
    if (AutomaticGrowthEvidence.CROP_TAG in evidence) return hasAge
    return hasAge &&
        AutomaticGrowthEvidence.BONEMEALABLE in evidence &&
        (AutomaticGrowthEvidence.SEED_TAG in evidence || AutomaticGrowthEvidence.VILLAGER_SEED_TAG in evidence)
}

private fun matureState(candidate: AutomaticGrowthCandidate): BlockState? {
    val block = candidate.block
    if (block is CropBlock) return block.getStateForAge(block.maxAge)
    val age = ageProperty(block) ?: return null
    val maximum = age.possibleValues.maxOrNull() ?: return null
    return block.defaultBlockState().setValue(age, maximum)
}

private fun ageProperty(block: Block): IntegerProperty? =
    block.stateDefinition.properties
        .asSequence()
        .filterIsInstance<IntegerProperty>()
        .firstOrNull { it.name == "age" }
