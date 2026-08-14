package rhx.lazy.feature.simulation

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.Level
import rhx.lazy.MOD_ID
import java.util.WeakHashMap

internal sealed interface ResolvedSimulation {
    val id: ResourceLocation
    val duration: Int

    data class ItemRecipe(
        val holder: RecipeHolder<ItemSimulationRecipe>,
    ) : ResolvedSimulation {
        override val id: ResourceLocation = holder.id()
        override val duration: Int = holder.value().durationTicks()
    }

    data class AutomaticMineral(
        override val id: ResourceLocation,
        override val duration: Int,
        val output: ItemStack,
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

internal data class AutomaticMineralDisplay(
    val input: ItemStack,
    val simulation: ResolvedSimulation.AutomaticMineral,
)

internal object SimulationRecipeResolver {
    private val indices = WeakHashMap<RecipeManager, RecipeIndex>()

    fun resolve(
        level: Level,
        stack: ItemStack,
    ): ResolvedSimulation? {
        if (stack.isEmpty) return null
        val index = indexFor(level)
        DataModelItem.entityTypeId(stack)?.let { entityId ->
            val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null) ?: return null
            if (entityType.`is`(SimulationTags.dataModelBlacklist)) return null
            val profile = index.entityRecipes[entityId]
            return ResolvedSimulation.EntityProfile(
                entityId,
                profile,
                profile?.value()?.durationTicks() ?: SimulationConfigs.settings.defaultDuration.get(),
            )
        }

        explicitItemRecipe(index, stack)?.let { return ResolvedSimulation.ItemRecipe(it) }
        return resolveAutomaticMineral(index, stack)
    }

    fun automaticMinerals(level: Level): List<AutomaticMineralDisplay> {
        val index = indexFor(level)
        refreshAutomaticSettings(index)
        if (!index.automaticSettings.enabled) return emptyList()
        index.automaticDisplays?.let { return it }
        return BuiltInRegistries.ITEM
            .asSequence()
            .map(::ItemStack)
            .mapNotNull { input ->
                val automatic = resolveAutomaticMineral(index, input) ?: return@mapNotNull null
                if (explicitItemRecipe(index, input) == null) AutomaticMineralDisplay(input, automatic) else null
            }.toList()
            .also { index.automaticDisplays = it }
    }

    @Synchronized
    fun invalidate() {
        indices.clear()
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
            RecipeIndex(itemRecipes, entityRecipes, currentAutomaticSettings())
        }
    }

    private fun explicitItemRecipe(
        index: RecipeIndex,
        stack: ItemStack,
    ): RecipeHolder<ItemSimulationRecipe>? {
        val key = TargetKey(stack)
        if (key in index.itemMatches) return index.itemMatches[key]
        return index.itemRecipes.firstOrNull { it.value().input.test(stack) }.also { index.itemMatches[key] = it }
    }

    private fun resolveAutomaticMineral(
        index: RecipeIndex,
        stack: ItemStack,
    ): ResolvedSimulation.AutomaticMineral? {
        refreshAutomaticSettings(index)
        if (!index.automaticSettings.enabled) return null
        val key = TargetKey(stack)
        if (key in index.automaticMatches) return index.automaticMatches[key]
        return resolveAutomaticMineralUncached(stack, index.automaticSettings.duration).also { index.automaticMatches[key] = it }
    }

    private fun resolveAutomaticMineralUncached(
        stack: ItemStack,
        duration: Int,
    ): ResolvedSimulation.AutomaticMineral? {
        if (stack.`is`(SimulationTags.automaticMineralBlacklist)) return null
        val itemId = BuiltInRegistries.ITEM.getKey(stack.item)
        val tags = stack.tags.toList()
        val ingots = tags.mapNotNull { material(it, "ingots/") }.distinct()
        val gems = tags.mapNotNull { material(it, "gems/") }.distinct()
        if (ingots.size + gems.size != 1) return null
        if (gems.size == 1) {
            return ResolvedSimulation.AutomaticMineral(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "automatic/gem/${gems.single()}"),
                duration,
                stack.copyWithCount(1),
            )
        }

        val material = ingots.single()
        val rawTag =
            TagKey.create(
                BuiltInRegistries.ITEM.key(),
                ResourceLocation.fromNamespaceAndPath("c", "raw_materials/$material"),
            )
        val output =
            BuiltInRegistries.ITEM
                .getTag(rawTag)
                .orElse(null)
                ?.asSequence()
                ?.map { it.value() }
                ?.sortedWith(
                    compareBy<Item>(
                        { namespaceRank(BuiltInRegistries.ITEM.getKey(it), itemId.namespace) },
                        { BuiltInRegistries.ITEM.getKey(it).toString() },
                    ),
                )?.firstOrNull() ?: return null
        return ResolvedSimulation.AutomaticMineral(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "automatic/ingot/$material"),
            duration,
            ItemStack(output),
        )
    }

    private fun material(
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

    private fun namespaceRank(
        id: ResourceLocation,
        preferred: String,
    ): Int =
        when (id.namespace) {
            preferred -> 0
            "minecraft" -> 1
            else -> 2
        }

    private fun refreshAutomaticSettings(index: RecipeIndex) {
        val settings = currentAutomaticSettings()
        if (settings == index.automaticSettings) return
        index.automaticSettings = settings
        index.automaticMatches.clear()
        index.automaticDisplays = null
    }

    private fun currentAutomaticSettings() =
        AutomaticSettings(
            SimulationConfigs.settings.automaticMinerals.get(),
            SimulationConfigs.settings.automaticMineralDuration.get(),
        )

    private class RecipeIndex(
        val itemRecipes: List<RecipeHolder<ItemSimulationRecipe>>,
        val entityRecipes: Map<ResourceLocation, RecipeHolder<EntitySimulationRecipe>>,
        var automaticSettings: AutomaticSettings,
    ) {
        val itemMatches = hashMapOf<TargetKey, RecipeHolder<ItemSimulationRecipe>?>()
        val automaticMatches = hashMapOf<TargetKey, ResolvedSimulation.AutomaticMineral?>()
        var automaticDisplays: List<AutomaticMineralDisplay>? = null
    }

    private data class AutomaticSettings(
        val enabled: Boolean,
        val duration: Int,
    )

    private data class TargetKey(
        val item: Item,
        val components: DataComponentPatch,
    ) {
        constructor(stack: ItemStack) : this(stack.item, stack.componentsPatch)
    }
}
