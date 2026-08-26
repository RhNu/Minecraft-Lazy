package rhx.lazy.feature.simulation

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.Level

/** All simulation paths are indexed as variants before the three context slots are matched. */
internal sealed interface SimulationVariant {
    val kind: SimulationVariantKind
    val id: ResourceLocation
    val group: ResourceLocation
    val priority: Int
    val tools: List<SimulationToolRequirement>

    data class Explicit(
        val holder: RecipeHolder<ItemSimulationRecipe>,
    ) : SimulationVariant {
        override val kind = SimulationVariantKind.EXPLICIT
        override val id: ResourceLocation = holder.id()
        override val group: ResourceLocation = holder.value().group
        override val priority: Int = holder.value().priority
        override val tools: List<SimulationToolRequirement> = holder.value().tools
    }

    data class Automatic(
        val candidate: AutomaticSimulationCandidate? = null,
        val snapshot: ResolvedSimulation.Item? = null,
    ) : SimulationVariant {
        init {
            require((candidate == null) != (snapshot == null))
        }

        override val kind = SimulationVariantKind.AUTOMATIC
        override val id: ResourceLocation = candidate?.id ?: requireNotNull(snapshot).id
        override val group: ResourceLocation = candidate?.group ?: requireNotNull(snapshot).group
        override val priority: Int = candidate?.priority ?: requireNotNull(snapshot).priority
        override val tools: List<SimulationToolRequirement> = candidate?.tools ?: requireNotNull(snapshot).tools
        val claimsInput: Boolean = candidate?.claimsInput ?: true
    }

    data class Entity(
        val holder: RecipeHolder<EntitySimulationRecipe>,
    ) : SimulationVariant {
        override val kind = SimulationVariantKind.ENTITY
        override val id: ResourceLocation = holder.id()
        override val group: ResourceLocation = holder.value().group
        override val priority: Int = holder.value().priority
        override val tools: List<SimulationToolRequirement> = holder.value().tools
    }

    data class Injection(
        val holder: RecipeHolder<ItemSimulationInjectionRecipe>,
    ) : SimulationVariant {
        override val kind = SimulationVariantKind.INJECTION
        override val id: ResourceLocation = holder.id()
        override val group: ResourceLocation = holder.value().group
        override val priority: Int = 0
        override val tools: List<SimulationToolRequirement> = holder.value().tools
    }
}

internal enum class SimulationVariantKind {
    EXPLICIT,
    AUTOMATIC,
    ENTITY,
    INJECTION,
}

internal class UnifiedSimulationVariantRegistry(
    manager: RecipeManager,
) {
    private val itemRecipes =
        manager
            .getAllRecipesFor(SimulationRegistries.itemRecipeType.get())
            .sortedBy { it.id().toString() }
    private val injections =
        manager
            .getAllRecipesFor(SimulationRegistries.itemInjectionRecipeType.get())
            .sortedBy { it.id().toString() }
    private val entityVariants =
        manager
            .getAllRecipesFor(SimulationRegistries.entityRecipeType.get())
            .groupBy { it.value().entity }
            .mapValues { (_, recipes) -> recipes.sortedBy { it.id().toString() }.map(SimulationVariant::Entity) }
    private val targetVariants = hashMapOf<TargetKey, List<SimulationVariant>>()
    private val staticToolRequirements =
        itemRecipes.flatMap { it.value().tools } +
            injections.flatMap { it.value().tools } +
            entityVariants.values.flatten().flatMap { it.tools }

    var automaticSettings: AutomaticSimulationSettings = AutomaticSimulationSettings.current()
        private set
    var automaticDisplays: List<AutomaticSimulationDisplay>? = null

    fun variants(
        level: Level,
        target: ItemStack,
    ): List<SimulationVariant> {
        refreshAutomaticSettings()
        return targetVariants.getOrPut(TargetKey(target)) {
            buildList {
                itemRecipes
                    .filter { it.value().input.test(target) }
                    .mapTo(this, SimulationVariant::Explicit)
                if (level.isClientSide) {
                    AutomaticSimulationClientSnapshot
                        .all()
                        .filter { it.input.item === target.item }
                        .mapTo(this) { SimulationVariant.Automatic(snapshot = it.simulation) }
                } else {
                    AutomaticSimulationAdapters
                        .candidates(level, target)
                        .mapTo(this) { SimulationVariant.Automatic(candidate = it) }
                }
                injections
                    .filter { it.value().input.test(target) }
                    .mapTo(this, SimulationVariant::Injection)
            }
        }
    }

    fun entityVariants(entity: ResourceLocation): List<SimulationVariant> = entityVariants[entity].orEmpty()

    fun acceptsContext(stack: ItemStack): Boolean =
        staticToolRequirements.any { it.matches(stack) } ||
            AutomaticSimulationAdapters.toolRequirements().any { it.matches(stack) }

    fun clearTargetCaches() {
        targetVariants.clear()
        automaticDisplays = null
    }

    private fun refreshAutomaticSettings() {
        val settings = AutomaticSimulationSettings.current()
        if (settings == automaticSettings) return
        automaticSettings = settings
        clearTargetCaches()
    }

    private data class TargetKey(
        val item: Item,
        val components: DataComponentPatch,
    ) {
        constructor(stack: ItemStack) : this(stack.item, stack.componentsPatch)
    }
}
