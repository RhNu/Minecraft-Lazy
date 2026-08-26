package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.Level
import rhx.lazy.LazyRuntime
import rhx.lazy.MOD_ID
import rhx.lazy.integration.api.LazyInternalApi
import java.util.WeakHashMap

@LazyInternalApi
public sealed interface ResolvedSimulation {
    val id: ResourceLocation
    val duration: Int
    val tools: List<SimulationToolRequirement>
    val group: ResourceLocation

    data class Item(
        override val id: ResourceLocation,
        override val duration: Int,
        val itemOutputs: List<SimulationItemOutput>,
        val fluidOutputs: List<SimulationFluidOutput>,
        val blockLootOutputs: List<SimulationBlockLootOutput> = emptyList(),
        override val tools: List<SimulationToolRequirement> = emptyList(),
        override val group: ResourceLocation = SimulationRecipeGroups.ITEM,
        val priority: Int = 0,
    ) : ResolvedSimulation

    data class EntityProfile(
        val entityId: ResourceLocation,
        val holder: RecipeHolder<EntitySimulationRecipe>?,
        override val duration: Int,
        override val tools: List<SimulationToolRequirement> = holder?.value()?.tools.orEmpty(),
    ) : ResolvedSimulation {
        override val id: ResourceLocation =
            holder?.id()
                ?: ResourceLocation.fromNamespaceAndPath(
                    MOD_ID,
                    "entity/${entityId.namespace}/${entityId.path}",
                )
        override val group: ResourceLocation = holder?.value()?.group ?: SimulationRecipeGroups.ENTITY
        val priority: Int = holder?.value()?.priority ?: Int.MIN_VALUE
    }
}

@LazyInternalApi
public data class AutomaticSimulationDisplay(
    val input: ItemStack,
    val simulation: ResolvedSimulation.Item,
)

@LazyInternalApi
public sealed interface SimulationResolution {
    public data class Success(
        val simulation: ResolvedSimulation,
    ) : SimulationResolution

    public data class Conflict(
        val ids: List<ResourceLocation>,
    ) : SimulationResolution

    public data object Unavailable : SimulationResolution
}

@LazyInternalApi
public data class SimulationInspectionCandidate(
    val kind: String,
    val id: ResourceLocation,
    val group: ResourceLocation,
    val priority: Int,
    val tools: List<SimulationToolRequirement>,
    val targetMatches: Boolean,
    val toolsMatch: Boolean,
)

@LazyInternalApi
public data class SimulationInspection(
    val candidates: List<SimulationInspectionCandidate>,
    val resolution: SimulationResolution,
)

@LazyInternalApi
public object SimulationRecipeResolver {
    private val registries = WeakHashMap<RecipeManager, UnifiedSimulationVariantRegistry>()

    fun resolve(
        level: Level,
        stack: ItemStack,
        tools: List<ItemStack> = emptyList(),
    ): ResolvedSimulation? =
        when (val result = resolveDetailed(level, stack, tools)) {
            is SimulationResolution.Success -> result.simulation
            is SimulationResolution.Conflict -> {
                logConflict(stack, tools, result.ids)
                null
            }
            SimulationResolution.Unavailable -> null
        }

    fun resolveDetailed(
        level: Level,
        stack: ItemStack,
        tools: List<ItemStack> = emptyList(),
    ): SimulationResolution {
        if (stack.isEmpty) return SimulationResolution.Unavailable
        val registry = registryFor(level)
        when (val targetResolution = EntitySimulationTargets.resolve(stack)) {
            EntitySimulationTargetResolution.InvalidEntityTarget -> return SimulationResolution.Unavailable
            EntitySimulationTargetResolution.NotEntityTarget -> Unit
            is EntitySimulationTargetResolution.Resolved -> {
                val target = targetResolution.target
                if (!target.isAllowed) return SimulationResolution.Unavailable
                return when (val selection = selectVariants(registry.entityVariants(target.id), tools)) {
                    is RankedSelection.Conflict -> SimulationResolution.Conflict(selection.values.map { it.id })
                    is RankedSelection.Selected -> {
                        val holder = (selection.value as SimulationVariant.Entity).holder
                        SimulationResolution.Success(
                            ResolvedSimulation.EntityProfile(target.id, holder, holder.value().durationTicks()),
                        )
                    }
                    RankedSelection.None ->
                        SimulationResolution.Success(
                            ResolvedSimulation.EntityProfile(
                                target.id,
                                null,
                                SimulationConfigs.settings.defaultDuration.get(),
                            ),
                        )
                }
            }
        }

        val variants = registry.variants(level, stack)
        val base: ResolvedSimulation.Item =
            when (val selection = selectVariants(variants.filterIsInstance<SimulationVariant.Explicit>(), tools)) {
                is RankedSelection.Conflict -> return SimulationResolution.Conflict(selection.values.map { it.id })
                is RankedSelection.Selected -> resolvedExplicit(selection.value.holder)
                RankedSelection.None -> {
                    val automatic = variants.filterIsInstance<SimulationVariant.Automatic>()
                    when (val automaticSelection = selectAutomaticVariants(automatic, tools)) {
                        is RankedSelection.Conflict ->
                            return SimulationResolution.Conflict(automaticSelection.values.map { it.id })
                        is RankedSelection.Selected ->
                            resolvedAutomatic(automaticSelection.value)
                                ?: return SimulationResolution.Unavailable
                        RankedSelection.None -> return SimulationResolution.Unavailable
                    }
                }
            }

        val selectedAutomatic = variants.filterIsInstance<SimulationVariant.Automatic>().firstOrNull { it.id == base.id }
        if (level.isClientSide && selectedAutomatic?.snapshot != null) return SimulationResolution.Success(base)
        val injections =
            variants
                .filterIsInstance<SimulationVariant.Injection>()
                .filter { simulationToolsMatch(it.tools, tools) }
                .map { it.holder }
        return composeItemSimulation(base, injections)
            ?.let { hydrateBlockLootDisplays(level, it) }
            ?.let(SimulationResolution::Success)
            ?: SimulationResolution.Unavailable
    }

    fun supportsTarget(
        level: Level,
        stack: ItemStack,
    ): Boolean {
        if (stack.isEmpty) return false
        when (val entity = EntitySimulationTargets.resolve(stack)) {
            EntitySimulationTargetResolution.InvalidEntityTarget -> return false
            EntitySimulationTargetResolution.NotEntityTarget -> Unit
            is EntitySimulationTargetResolution.Resolved -> return entity.target.isAllowed
        }
        return registryFor(level)
            .variants(level, stack)
            .any { it.kind == SimulationVariantKind.EXPLICIT || it.kind == SimulationVariantKind.AUTOMATIC }
    }

    fun supportsTool(
        level: Level,
        stack: ItemStack,
    ): Boolean = !stack.isEmpty && registryFor(level).acceptsContext(stack)

    fun inspect(
        level: Level,
        stack: ItemStack,
        tools: List<ItemStack> = emptyList(),
    ): SimulationInspection {
        val registry = registryFor(level)
        val variants =
            when (val target = EntitySimulationTargets.resolve(stack)) {
                is EntitySimulationTargetResolution.Resolved -> registry.entityVariants(target.target.id)
                else -> registry.variants(level, stack)
            }
        return SimulationInspection(
            variants
                .map { variant ->
                    SimulationInspectionCandidate(
                        variant.kind.name.lowercase(),
                        variant.id,
                        variant.group,
                        variant.priority,
                        variant.tools,
                        true,
                        simulationToolsMatch(variant.tools, tools),
                    )
                }.sortedWith(compareBy({ it.kind }, { it.group.toString() }, { it.id.toString() })),
            resolveDetailed(level, stack, tools),
        )
    }

    fun automaticSimulations(level: Level): List<AutomaticSimulationDisplay> {
        if (level.isClientSide) return AutomaticSimulationClientSnapshot.all()
        val registry = registryFor(level)
        registry.automaticDisplays?.let { return it }
        return BuiltInRegistries.ITEM
            .asSequence()
            .map(::ItemStack)
            .flatMap { input ->
                val variants = registry.variants(level, input)
                val injections = variants.filterIsInstance<SimulationVariant.Injection>()
                variants
                    .filterIsInstance<SimulationVariant.Automatic>()
                    .mapNotNull { automatic ->
                        val base = resolvedAutomatic(automatic) ?: return@mapNotNull null
                        val sampleTools = automatic.tools.mapNotNull(::displayStackFor)
                        val matches = injections.filter { simulationToolsMatch(it.tools, sampleTools) }.map { it.holder }
                        composeItemSimulation(base, matches)
                            ?.let { hydrateBlockLootDisplays(level, it) }
                            ?.let { AutomaticSimulationDisplay(input, it) }
                    }.asSequence()
            }.toList()
            .also { registry.automaticDisplays = it }
    }

    @Synchronized
    fun invalidate() {
        registries.clear()
        AutomaticGrowthIndex.invalidate()
        SimulationLootDisplays.invalidate()
    }

    @Synchronized
    internal fun invalidateTargetCaches() {
        registries.values.forEach(UnifiedSimulationVariantRegistry::clearTargetCaches)
    }

    @Synchronized
    private fun registryFor(level: Level): UnifiedSimulationVariantRegistry =
        registries.getOrPut(level.recipeManager) { UnifiedSimulationVariantRegistry(level.recipeManager) }

    private fun validOutputCount(simulation: ResolvedSimulation.Item): Boolean {
        val count = effectiveOutputCount(simulation.itemOutputs, simulation.fluidOutputs, simulation.blockLootOutputs)
        if (count <= MAX_OUTPUT_ENTRIES) return true
        LazyRuntime.logger.error(
            "Disabled simulation {} because its composed output count {} exceeds {}",
            simulation.id,
            count,
            MAX_OUTPUT_ENTRIES,
        )
        return false
    }

    private fun resolvedExplicit(holder: RecipeHolder<ItemSimulationRecipe>): ResolvedSimulation.Item {
        val recipe = holder.value()
        return ResolvedSimulation.Item(
            holder.id(),
            recipe.durationTicks(),
            recipe.itemOutputs.map(::copy),
            recipe.fluidOutputs.map(::copy),
            recipe.blockLootOutputs.map(::copy),
            recipe.tools,
            recipe.group,
            recipe.priority,
        )
    }

    private fun resolvedAutomatic(variant: SimulationVariant.Automatic): ResolvedSimulation.Item? {
        variant.snapshot?.let { return it }
        val candidate = requireNotNull(variant.candidate)
        return ResolvedSimulation
            .Item(
                candidate.id,
                candidate.duration,
                candidate.itemOutputs.map(::copy),
                candidate.fluidOutputs.map(::copy),
                candidate.blockLootOutputs.map(::copy),
                candidate.tools,
                candidate.group,
                candidate.priority,
            ).takeIf(::validOutputCount)
    }

    private fun hydrateBlockLootDisplays(
        level: Level,
        simulation: ResolvedSimulation.Item,
    ): ResolvedSimulation.Item =
        simulation.copy(
            blockLootOutputs =
                simulation.blockLootOutputs.map { output ->
                    if (output.displayItems.isNotEmpty()) {
                        copy(output)
                    } else {
                        output.copy(
                            displayItems = SimulationLootDisplays.items(level, output.state, output.tool),
                            tool = output.tool.copy(),
                        )
                    }
                },
        )

    private fun copy(output: SimulationItemOutput) = output.copy(stack = output.stack.copy())

    private fun copy(output: SimulationFluidOutput) = output.copy(stack = output.stack.copy())

    private fun copy(output: SimulationBlockLootOutput) =
        output.copy(displayItems = output.displayItems.map(ItemStack::copy), tool = output.tool.copy())

    private val loggedConflicts = hashSetOf<String>()

    private fun logConflict(
        stack: ItemStack,
        tools: List<ItemStack>,
        ids: List<ResourceLocation>,
    ) {
        val key =
            "${BuiltInRegistries.ITEM.getKey(stack.item)}|" +
                "${tools.joinToString { BuiltInRegistries.ITEM.getKey(it.item).toString() }}|${ids.joinToString()}"
        if (!loggedConflicts.add(key)) return
        LazyRuntime.logger.error("Simulation recipe conflict for {} with tools {}: {}", stack, tools, ids)
    }
}

internal fun selectExplicitSimulation(
    recipes: List<RecipeHolder<ItemSimulationRecipe>>,
    stack: ItemStack,
    tools: List<ItemStack> = emptyList(),
): RankedSelection<RecipeHolder<ItemSimulationRecipe>> =
    selectRecipe(recipes.filter { it.value().input.test(stack) }, tools) { it.value().priority to it.value().tools }

internal sealed interface RankedSelection<out T> {
    data class Selected<T>(
        val value: T,
    ) : RankedSelection<T>

    data class Conflict<T>(
        val values: List<T>,
    ) : RankedSelection<T>

    data object None : RankedSelection<Nothing>
}

private fun <T : SimulationVariant> selectVariants(
    values: List<T>,
    tools: List<ItemStack>,
): RankedSelection<T> = selectRanked(values.filter { simulationToolsMatch(it.tools, tools) }) { it.priority to it.tools }

private fun <T> selectRecipe(
    values: List<T>,
    tools: List<ItemStack>,
    rank: (T) -> Pair<Int, List<SimulationToolRequirement>>,
): RankedSelection<T> = selectRanked(values.filter { simulationToolsMatch(rank(it).second, tools) }, rank)

private fun <T> selectRanked(
    matches: List<T>,
    rank: (T) -> Pair<Int, List<SimulationToolRequirement>>,
): RankedSelection<T> {
    if (matches.isEmpty()) return RankedSelection.None
    val bestPriority = matches.maxOf { rank(it).first }
    val priorityMatches = matches.filter { rank(it).first == bestPriority }
    val bestToolCount = priorityMatches.maxOf { rank(it).second.size }
    val best = priorityMatches.filter { rank(it).second.size == bestToolCount }
    return if (best.size == 1) RankedSelection.Selected(best.single()) else RankedSelection.Conflict(best)
}

private fun selectAutomaticVariants(
    variants: List<SimulationVariant.Automatic>,
    tools: List<ItemStack>,
): RankedSelection<SimulationVariant.Automatic> {
    val matches = variants.filter { simulationToolsMatch(it.tools, tools) }
    val claimed = matches.filter(SimulationVariant.Automatic::claimsInput)
    return selectRanked(if (claimed.isEmpty()) matches else claimed) { it.priority to it.tools }
}

private fun displayStackFor(requirement: SimulationToolRequirement): ItemStack? =
    when (requirement) {
        is SimulationToolRequirement.Item ->
            requirement.ingredient.items
                .firstOrNull()
                ?.copy()
        is SimulationToolRequirement.BlockTag ->
            BuiltInRegistries.BLOCK
                .getTag(requirement.tag)
                .orElse(null)
                ?.firstOrNull()
                ?.value()
                ?.asItem()
                ?.let(::ItemStack)
    }

internal fun composeItemSimulation(
    base: ResolvedSimulation.Item?,
    injections: List<RecipeHolder<ItemSimulationInjectionRecipe>>,
): ResolvedSimulation.Item? {
    base ?: return null
    val sorted = injections.sortedBy { it.id().toString() }
    val result =
        base.copy(
            itemOutputs = base.itemOutputs + sorted.flatMap { it.value().itemOutputs }.map(::copyItemOutput),
            fluidOutputs = base.fluidOutputs + sorted.flatMap { it.value().fluidOutputs }.map(::copyFluidOutput),
            blockLootOutputs = base.blockLootOutputs + sorted.flatMap { it.value().blockLootOutputs }.map(::copyBlockLootOutput),
        )
    val count = effectiveOutputCount(result.itemOutputs, result.fluidOutputs, result.blockLootOutputs)
    if (count <= MAX_OUTPUT_ENTRIES) return result
    LazyRuntime.logger.error(
        "Disabled simulation {} because its composed output count {} exceeds {}",
        result.id,
        count,
        MAX_OUTPUT_ENTRIES,
    )
    return null
}

private fun copyItemOutput(output: SimulationItemOutput) = output.copy(stack = output.stack.copy())

private fun copyFluidOutput(output: SimulationFluidOutput) = output.copy(stack = output.stack.copy())

private fun copyBlockLootOutput(output: SimulationBlockLootOutput) =
    output.copy(displayItems = output.displayItems.map(ItemStack::copy), tool = output.tool.copy())
