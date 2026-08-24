package rhx.lazy.core.material

import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * Shared ordered selection for tag derived outputs. Every generator that can see more than one
 * candidate behind a tag picks through here, so a single server setting decides the winner.
 */
internal object MaterialTagPreference {
    fun namespaces(): List<String> = MaterialConfigs.settings.modPriority.get()

    fun preferredItem(tag: TagKey<Item>): Item? =
        BuiltInRegistries.ITEM
            .getTag(tag)
            .orElse(null)
            ?.let { candidates -> preferredItem(candidates, materialIdComparator(namespaces())) }

    /**
     * Takes a prebuilt comparator so a whole-registry sweep pays for the namespace lookup once
     * instead of once per tag.
     */
    fun preferredItem(
        candidates: Iterable<Holder<Item>>,
        comparator: Comparator<ResourceLocation>,
    ): Item? =
        candidates
            .asSequence()
            .map(Holder<Item>::value)
            .filterNot { it === Items.AIR }
            .minWithOrNull { first, second ->
                comparator.compare(BuiltInRegistries.ITEM.getKey(first), BuiltInRegistries.ITEM.getKey(second))
            }
}

/** Configured namespaces first, then every other namespace and full item id in ascending order. */
internal fun materialIdComparator(priorities: List<String>): Comparator<ResourceLocation> =
    compareBy<ResourceLocation>(
        { id -> priorities.indexOf(id.namespace).let { if (it < 0) Int.MAX_VALUE else it } },
        ResourceLocation::getNamespace,
        ResourceLocation::toString,
    )
