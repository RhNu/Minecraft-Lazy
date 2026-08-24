package rhx.lazy.feature.simulation

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
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

    data class Item(
        override val id: ResourceLocation,
        override val duration: Int,
        val itemOutputs: List<SimulationItemOutput>,
        val fluidOutputs: List<SimulationFluidOutput>,
        val blockLootOutputs: List<SimulationBlockLootOutput> = emptyList(),
    ) : ResolvedSimulation

    data class EntityProfile(
        val entityId: ResourceLocation,
        val holder: RecipeHolder<EntitySimulationRecipe>?,
        override val duration: Int,
    ) : ResolvedSimulation {
        override val id: ResourceLocation =
            holder?.id()
                ?: ResourceLocation.fromNamespaceAndPath(
                    MOD_ID,
                    "entity/${entityId.namespace}/${entityId.path}",
                )
    }
}

@LazyInternalApi
public data class AutomaticSimulationDisplay(
    val input: ItemStack,
    val simulation: ResolvedSimulation.Item,
)

@LazyInternalApi
public object SimulationRecipeResolver {
    private val indices = WeakHashMap<RecipeManager, RecipeIndex>()

    fun resolve(
        level: Level,
        stack: ItemStack,
    ): ResolvedSimulation? {
        if (stack.isEmpty) return null
        val index = indexFor(level)
        when (val resolution = EntitySimulationTargets.resolve(stack)) {
            EntitySimulationTargetResolution.InvalidEntityTarget -> return null
            EntitySimulationTargetResolution.NotEntityTarget -> Unit
            is EntitySimulationTargetResolution.Resolved -> {
                val target = resolution.target
                if (!target.isAllowed) return null
                val profile = index.entityRecipes[target.id]
                return ResolvedSimulation.EntityProfile(
                    target.id,
                    profile,
                    profile?.value()?.durationTicks() ?: SimulationConfigs.settings.defaultDuration.get(),
                )
            }
        }

        explicitItemRecipe(index, stack)?.let { return applyInjections(index, stack, resolvedExplicit(it)) }
        if (level.isClientSide) return AutomaticSimulationClientSnapshot.find(stack)
        return automaticItemRecipe(level, index, stack)?.let { applyInjections(index, stack, it) }
    }

    fun automaticSimulations(level: Level): List<AutomaticSimulationDisplay> {
        if (level.isClientSide) return AutomaticSimulationClientSnapshot.all()
        val index = indexFor(level)
        refreshAutomaticSettings(index)
        index.automaticDisplays?.let { return it }
        return BuiltInRegistries.ITEM
            .asSequence()
            .map(::ItemStack)
            .mapNotNull { input ->
                if (explicitItemRecipe(index, input) != null) return@mapNotNull null
                val automatic = automaticItemRecipe(level, index, input) ?: return@mapNotNull null
                val effective = applyInjections(index, input, automatic) ?: return@mapNotNull null
                AutomaticSimulationDisplay(input, effective)
            }.toList()
            .also { index.automaticDisplays = it }
    }

    @Synchronized
    fun invalidate() {
        indices.clear()
        AutomaticGrowthIndex.invalidate()
        SimulationLootDisplays.invalidate()
    }

    @Synchronized
    private fun indexFor(level: Level): RecipeIndex {
        val manager = level.recipeManager
        return indices.getOrPut(manager) {
            val itemRecipes =
                manager
                    .getAllRecipesFor(SimulationRegistries.itemRecipeType.get())
                    .sortedWith(
                        compareByDescending<RecipeHolder<ItemSimulationRecipe>> { it.value().priority }.thenBy { it.id().toString() },
                    )
            val injections =
                manager
                    .getAllRecipesFor(SimulationRegistries.itemInjectionRecipeType.get())
                    .sortedBy { it.id().toString() }
            val entityRecipes =
                manager
                    .getAllRecipesFor(SimulationRegistries.entityRecipeType.get())
                    .groupBy { it.value().entity }
                    .mapValues { (_, recipes) ->
                        recipes
                            .sortedWith(
                                compareByDescending<RecipeHolder<EntitySimulationRecipe>> { it.value().priority }
                                    .thenBy { it.id().toString() },
                            ).first()
                    }
            RecipeIndex(itemRecipes, injections, entityRecipes, AutomaticSimulationSettings.current())
        }
    }

    private fun explicitItemRecipe(
        index: RecipeIndex,
        stack: ItemStack,
    ): RecipeHolder<ItemSimulationRecipe>? {
        val key = TargetKey(stack)
        if (key in index.itemMatches) return index.itemMatches[key]
        return selectExplicitSimulation(index.itemRecipes, stack).also { index.itemMatches[key] = it }
    }

    private fun automaticItemRecipe(
        level: Level,
        index: RecipeIndex,
        stack: ItemStack,
    ): ResolvedSimulation.Item? {
        refreshAutomaticSettings(index)
        val key = TargetKey(stack)
        if (key in index.automaticMatches) return index.automaticMatches[key]
        val candidates = AutomaticSimulationAdapters.resolve(level, stack)
        if (candidates.isEmpty()) {
            index.automaticMatches[key] = null
            return null
        }
        val primary = candidates.first()
        val resolved =
            ResolvedSimulation
                .Item(
                    primary.id,
                    primary.duration,
                    candidates.flatMap(AutomaticSimulationCandidate::itemOutputs).map(::copy),
                    candidates.flatMap(AutomaticSimulationCandidate::fluidOutputs).map(::copy),
                    candidates.flatMap(AutomaticSimulationCandidate::blockLootOutputs).map(::copy),
                ).takeIf(::validOutputCount)
        index.automaticMatches[key] = resolved
        return resolved
    }

    private fun applyInjections(
        index: RecipeIndex,
        stack: ItemStack,
        base: ResolvedSimulation.Item,
    ): ResolvedSimulation.Item? {
        val key = TargetKey(stack)
        val matches =
            index.injectionMatches.getOrPut(key) {
                index.injections.filter { it.value().input.test(stack) }
            }
        return composeItemSimulation(base, matches)
    }

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
        )
    }

    private fun refreshAutomaticSettings(index: RecipeIndex) {
        val settings = AutomaticSimulationSettings.current()
        if (settings == index.automaticSettings) return
        index.automaticSettings = settings
        index.automaticMatches.clear()
        index.automaticDisplays = null
    }

    private fun copy(output: SimulationItemOutput) = output.copy(stack = output.stack.copy())

    private fun copy(output: SimulationFluidOutput) = output.copy(stack = output.stack.copy())

    private fun copy(output: SimulationBlockLootOutput) =
        output.copy(displayItems = output.displayItems.map(ItemStack::copy), tool = output.tool.copy())

    private class RecipeIndex(
        val itemRecipes: List<RecipeHolder<ItemSimulationRecipe>>,
        val injections: List<RecipeHolder<ItemSimulationInjectionRecipe>>,
        val entityRecipes: Map<ResourceLocation, RecipeHolder<EntitySimulationRecipe>>,
        var automaticSettings: AutomaticSimulationSettings,
    ) {
        val itemMatches = hashMapOf<TargetKey, RecipeHolder<ItemSimulationRecipe>?>()
        val injectionMatches = hashMapOf<TargetKey, List<RecipeHolder<ItemSimulationInjectionRecipe>>>()
        val automaticMatches = hashMapOf<TargetKey, ResolvedSimulation.Item?>()
        var automaticDisplays: List<AutomaticSimulationDisplay>? = null
    }

    private data class TargetKey(
        val item: Item,
        val components: DataComponentPatch,
    ) {
        constructor(stack: ItemStack) : this(stack.item, stack.componentsPatch)
    }
}

internal fun selectExplicitSimulation(
    recipes: List<RecipeHolder<ItemSimulationRecipe>>,
    stack: ItemStack,
): RecipeHolder<ItemSimulationRecipe>? =
    recipes
        .asSequence()
        .filter { it.value().input.test(stack) }
        .minWithOrNull(
            compareByDescending<RecipeHolder<ItemSimulationRecipe>> { it.value().priority }
                .thenBy { it.id().toString() },
        )

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
