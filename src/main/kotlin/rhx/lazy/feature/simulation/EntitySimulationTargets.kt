package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SpawnEggItem

/**
 * Resolves every item representation of an entity simulation target.
 *
 * A bound data model and a spawn egg are only two carriers for the same entity type. Keeping that
 * distinction out of recipe resolution makes input validation, blacklisting, displays and recipe
 * viewers agree on what the chamber will simulate.
 */
internal data class EntitySimulationTarget(
    val id: ResourceLocation,
    val type: EntityType<*>,
) {
    val isAllowed: Boolean
        get() = EntitySimulationTargets.isAllowed(type)
}

internal sealed interface EntitySimulationTargetResolution {
    data object NotEntityTarget : EntitySimulationTargetResolution

    data object InvalidEntityTarget : EntitySimulationTargetResolution

    data class Resolved(
        val target: EntitySimulationTarget,
    ) : EntitySimulationTargetResolution
}

internal object EntitySimulationTargets {
    fun resolve(stack: ItemStack): EntitySimulationTargetResolution {
        if (stack.isEmpty) return EntitySimulationTargetResolution.NotEntityTarget
        val item = stack.item
        if (item is SpawnEggItem) return resolved(item.getType(stack))
        val entityId = DataModelItem.entityTypeId(stack) ?: return EntitySimulationTargetResolution.NotEntityTarget
        val type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null)
        return type?.let { EntitySimulationTargetResolution.Resolved(EntitySimulationTarget(entityId, it)) }
            ?: EntitySimulationTargetResolution.InvalidEntityTarget
    }

    fun isAllowed(type: EntityType<*>): Boolean = !type.`is`(SimulationTags.entityTargetBlacklist)

    fun representative(
        target: EntitySimulationTarget,
        fallback: ItemStack,
    ): ItemStack = SpawnEggItem.byId(target.type)?.let(::ItemStack) ?: fallback.copyWithCount(1)

    fun equivalentInputs(entityId: ResourceLocation): List<ItemStack> {
        val type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null) ?: return emptyList()
        return buildList {
            SpawnEggItem.byId(type)?.let { add(ItemStack(it)) }
            add(DataModelItem.boundTo(entityId))
        }
    }

    private fun resolved(type: EntityType<*>): EntitySimulationTargetResolution {
        val id =
            BuiltInRegistries.ENTITY_TYPE
                .getResourceKey(type)
                .orElse(null)
                ?.location()
                ?: return EntitySimulationTargetResolution.InvalidEntityTarget
        return EntitySimulationTargetResolution.Resolved(EntitySimulationTarget(id, type))
    }
}
