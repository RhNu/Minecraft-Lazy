package rhx.lazy.feature.shaping

import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import rhx.lazy.core.lazyId

/**
 * Pack-side escape hatches.
 *
 * The unit table is a global convention, and a pack's real recipes may disagree with it. When the
 * table values a form higher than the pack's own recipe cost, a round trip nets material — Lazy does
 * not care about balance, but a pack that does needs a way to shut a specific case down without
 * editing the form table.
 *
 * Both tags ship empty; they only exist so a pack has somewhere to write.
 */
internal object ShaperTags {
    /** Items the shaper refuses to accept, even when they carry a known `c:` form tag. */
    val inputBlacklist: TagKey<Item> = itemTag("shaper/blacklist/input")

    /** Items the shaper refuses to produce; the lane idles instead of converting. */
    val outputBlacklist: TagKey<Item> = itemTag("shaper/blacklist/output")

    private fun itemTag(path: String): TagKey<Item> = TagKey.create(Registries.ITEM, lazyId(path))
}
