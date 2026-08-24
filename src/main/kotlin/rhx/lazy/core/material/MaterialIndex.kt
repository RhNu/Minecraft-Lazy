package rhx.lazy.core.material

import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/** What a single item is: which material, in which form, worth how many units. */
internal data class MaterialFormMatch(
    val material: String,
    val form: ResourceKey<MaterialForm>,
    val units: Int,
)

private typealias FormEntry = Pair<ResourceKey<MaterialForm>, MaterialForm>

/**
 * The resolved material table: every `c:` tag that names a known form, flattened into two O(1)
 * lookups.
 *
 * Built once whenever tags or the priority setting change, never during a tick. That is the whole
 * point — a machine converting eight lanes per tick cannot afford to walk tags, so it walks a
 * [HashMap] instead and the sweep happens on reload, where a few milliseconds do not matter.
 */
internal class MaterialIndex private constructor(
    private val byItem: Map<Item, MaterialFormMatch>,
    private val byMaterial: Map<String, Map<ResourceKey<MaterialForm>, Item>>,
    private val unitsByForm: Map<ResourceKey<MaterialForm>, Int>,
    /** Forms in a stable display order: cheapest first, ties broken by id. */
    val forms: List<ResourceKey<MaterialForm>>,
) {
    val materials: Set<String>
        get() = byMaterial.keys

    /** Null for items with no known form, and for items that two different forms both claim. */
    fun formOf(item: Item): MaterialFormMatch? = byItem[item]

    fun itemFor(
        material: String,
        form: ResourceKey<MaterialForm>,
    ): Item? = byMaterial[material]?.get(form)

    /** Every form this material actually has an item for. */
    fun formsOf(material: String): Map<ResourceKey<MaterialForm>, Item> = byMaterial[material].orEmpty()

    fun unitsOf(form: ResourceKey<MaterialForm>): Int? = unitsByForm[form]

    companion object {
        val EMPTY = MaterialIndex(emptyMap(), emptyMap(), emptyMap(), emptyList())

        fun build(registries: RegistryAccess): MaterialIndex {
            val registry = registries.registry(MaterialForms.REGISTRY_KEY).orElse(null) ?: return EMPTY
            val entries: List<FormEntry> = registry.entrySet().map { entry -> entry.key to entry.value }
            if (entries.isEmpty()) return EMPTY

            // Longest literal prefix first: `c:storage_blocks/raw_iron` has to read as iron's raw
            // block, not as a storage block of a material called `raw_iron`. Both forms match that
            // tag, and the more specific one is always the intended one.
            val matchOrder =
                entries.sortedWith(
                    compareByDescending<FormEntry> { it.second.pathPrefix.length }
                        .thenBy { it.first.location().toString() },
                )
            val comparator = materialIdComparator(MaterialTagPreference.namespaces())

            val byItem = HashMap<Item, MaterialFormMatch>()
            val ambiguous = HashSet<Item>()
            val byMaterial = HashMap<String, MutableMap<ResourceKey<MaterialForm>, Item>>()

            BuiltInRegistries.ITEM.tags.toList().forEach tags@{ tagged ->
                val match = matchTag(matchOrder, tagged.first.location) ?: return@tags
                val holders = tagged.second
                MaterialTagPreference.preferredItem(holders, comparator)?.let { chosen ->
                    byMaterial.getOrPut(match.material) { LinkedHashMap() }[match.form] = chosen
                }
                holders.forEach holders@{ holder ->
                    val item = holder.value()
                    if (item === Items.AIR) return@holders
                    val existing = byItem.put(item, match)
                    // An item that two forms both claim is a tagging mistake somewhere. Refuse to
                    // guess rather than pick a side, the same way the simulation chamber does.
                    if (existing != null && existing != match) ambiguous += item
                }
            }
            ambiguous.forEach(byItem::remove)

            return MaterialIndex(
                byItem,
                byMaterial,
                entries.associate { (key, form) -> key to form.units },
                entries
                    .sortedWith(compareBy<FormEntry> { it.second.units }.thenBy { it.first.location().toString() })
                    .map(FormEntry::first),
            )
        }

        private fun matchTag(
            matchOrder: List<FormEntry>,
            tagId: ResourceLocation,
        ): MaterialFormMatch? {
            matchOrder.forEach { (key, form) ->
                val material = form.materialOf(tagId)
                if (material != null) return MaterialFormMatch(material, key, form.units)
            }
            return null
        }
    }
}

/**
 * Holds the current [MaterialIndex].
 *
 * There is exactly one build path — [refresh], driven by the tag reload — so the index a machine
 * reads is always the one the reload produced. That matters in single player, where the client and
 * the integrated server share this object: if both were allowed to build from their own registries,
 * they would overwrite each other every tick.
 *
 * The registries used for the last build are kept weakly, only so a configuration change can rebuild
 * without waiting for another reload. A world that has been left is free to be collected; losing the
 * reference costs nothing more than skipping a rebuild that no longer has anything to rebuild for.
 */
internal object MaterialIndexes {
    @Volatile
    private var index: MaterialIndex = MaterialIndex.EMPTY

    @Volatile
    private var source: WeakReference<RegistryAccess>? = null
    private val listeners = CopyOnWriteArrayList<(MaterialIndex) -> Unit>()

    fun current(): MaterialIndex = index

    fun refresh(registries: RegistryAccess) {
        source = WeakReference(registries)
        publish(MaterialIndex.build(registries))
    }

    /** Rebuilds against the registries of the last [refresh]; a no-op before the first one. */
    fun rebuild() {
        val registries = source?.get() ?: return
        publish(MaterialIndex.build(registries))
    }

    fun addListener(listener: (MaterialIndex) -> Unit) {
        listeners += listener
        listener(index)
    }

    private fun publish(next: MaterialIndex) {
        index = next
        listeners.forEach { listener -> listener(next) }
    }
}
