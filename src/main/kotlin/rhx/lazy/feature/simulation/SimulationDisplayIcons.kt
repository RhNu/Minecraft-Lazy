package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SpawnEggItem

/**
 * The icon the chamber shows for a target.
 *
 * An item target identifies itself: a sapling looks like a sapling, an ingot like an ingot. A data
 * model does not — every bound model is the same card — so the entity behind it is swapped in for
 * its spawn egg, which players already read as "this mob". Entities that never got an egg, and
 * bindings whose mod has since left the pack, fall back to the card itself; its glint at least says
 * an entity is bound.
 */
internal object SimulationDisplayIcons {
    fun iconFor(target: ItemStack): ItemStack {
        if (target.isEmpty) return ItemStack.EMPTY
        val entityId = DataModelItem.entityTypeId(target) ?: return target.copyWithCount(1)
        val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null)
        val egg = entityType?.let(SpawnEggItem::byId)
        return egg?.let(::ItemStack) ?: target.copyWithCount(1)
    }
}
