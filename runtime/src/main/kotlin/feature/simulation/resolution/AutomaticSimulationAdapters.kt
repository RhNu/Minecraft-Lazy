package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import rhx.lazy.core.lazyId
import rhx.lazy.core.material.MaterialTagPreference
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public data class AutomaticSimulationCandidate(
    val source: ResourceLocation,
    val id: ResourceLocation,
    val duration: Int,
    val priority: Int,
    val claimsInput: Boolean = false,
    val itemOutputs: List<SimulationItemOutput> = emptyList(),
    val fluidOutputs: List<SimulationFluidOutput> = emptyList(),
    val blockLootOutputs: List<SimulationBlockLootOutput> = emptyList(),
    val tools: List<SimulationToolRequirement> = emptyList(),
    val group: ResourceLocation = source,
)

@LazyInternalApi
public fun interface AutomaticSimulationAdapter {
    fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate?

    fun settingsFingerprint(): Any? = null

    fun toolRequirements(): List<SimulationToolRequirement> = emptyList()
}

@LazyInternalApi
public object AutomaticSimulationAdapters {
    private val sources = linkedMapOf<ResourceLocation, Registration>()

    init {
        register(TreeSimulationAdapter.SOURCE, TreeSimulationAdapter)
        register(CropSimulationAdapter.SOURCE, CropSimulationAdapter)
        register(PlantSimulationAdapter.SOURCE, PlantSimulationAdapter)
        register(TaggedMaterialAdapter.SOURCE, TaggedMaterialAdapter)
    }

    @Synchronized
    fun register(
        source: ResourceLocation,
        adapter: AutomaticSimulationAdapter,
    ) {
        val registration = Registration(adapter, SimulationTags.sourceBlacklist(source))
        require(sources.putIfAbsent(source, registration) == null) { "Duplicate automatic simulation source $source" }
        SimulationRecipeResolver.invalidate()
    }

    fun resolve(
        level: Level,
        stack: ItemStack,
        tools: List<ItemStack> = emptyList(),
    ): List<AutomaticSimulationCandidate> {
        val candidates = candidates(level, stack).filter { simulationToolsMatch(it.tools, tools) }
        return selectAutomaticSimulationCandidate(candidates)?.let(::listOf).orEmpty()
    }

    fun candidates(
        level: Level,
        stack: ItemStack,
    ): List<AutomaticSimulationCandidate> {
        if (stack.`is`(SimulationTags.blacklist)) return emptyList()
        return synchronized(this) { sources.values.toList() }
            .filterNot { registration -> stack.`is`(registration.blacklist) }
            .mapNotNull { registration -> registration.adapter.resolve(level, stack) }
    }

    fun settingsFingerprint(): List<Pair<ResourceLocation, Any?>> =
        synchronized(this) { sources.map { (source, registration) -> source to registration.adapter.settingsFingerprint() } }

    fun toolRequirements(): List<SimulationToolRequirement> =
        synchronized(this) { sources.values.flatMap { it.adapter.toolRequirements() } }

    private class Registration(
        val adapter: AutomaticSimulationAdapter,
        val blacklist: TagKey<Item>,
    )
}

internal fun selectAutomaticSimulationCandidate(candidates: List<AutomaticSimulationCandidate>): AutomaticSimulationCandidate? {
    val claimed = candidates.filter(AutomaticSimulationCandidate::claimsInput)
    return (if (claimed.isEmpty()) candidates else claimed).minWithOrNull(candidateComparator)
}

private val candidateComparator =
    compareByDescending<AutomaticSimulationCandidate>(AutomaticSimulationCandidate::priority)
        .thenByDescending { it.tools.size }
        .thenBy { it.source.toString() }
        .thenBy { it.id.toString() }

/** Every automatic recipe id is `lazy:automatic/<source path>/<segments...>`. */
@LazyInternalApi
public fun automaticId(
    source: ResourceLocation,
    vararg segments: String,
): ResourceLocation = lazyId((listOf("automatic", source.path) + segments).joinToString("/"))

/**
 * Configuration inputs shared by every automatic source. Cheap enough to sample on every resolve,
 * unlike [AutomaticSimulationAdapters.settingsFingerprint].
 */
internal data class AutomaticSimulationSettings(
    val defaultDuration: Int,
    val materialsEnabled: Boolean,
    val materialDuration: Int,
    val modPriority: List<String>,
    val materialRules: List<String>,
) {
    companion object {
        fun current(): AutomaticSimulationSettings =
            AutomaticSimulationSettings(
                SimulationConfigs.settings.defaultDuration.get(),
                SimulationConfigs.settings.taggedMaterials.get(),
                SimulationConfigs.settings.taggedMaterialDuration.get(),
                MaterialTagPreference.namespaces(),
                TaggedMaterialRules.fingerprint(),
            )
    }
}
