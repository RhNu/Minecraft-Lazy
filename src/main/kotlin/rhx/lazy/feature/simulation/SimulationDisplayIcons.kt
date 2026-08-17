package rhx.lazy.feature.simulation

import net.minecraft.world.item.ItemStack

/**
 * The icon the chamber shows for a target.
 *
 * An item target identifies itself: a sapling looks like a sapling, an ingot like an ingot. Every
 * representation of an entity target resolves to that entity's spawn egg, which players already
 * read as "this mob". Entities that never got an egg, and bindings whose mod has since left the
 * pack, fall back to the original target.
 */
internal object SimulationDisplayIcons {
    fun iconFor(target: ItemStack): ItemStack {
        if (target.isEmpty) return ItemStack.EMPTY
        val resolution = EntitySimulationTargets.resolve(target)
        return if (resolution is EntitySimulationTargetResolution.Resolved) {
            EntitySimulationTargets.representative(resolution.target, target)
        } else {
            target.copyWithCount(1)
        }
    }
}
