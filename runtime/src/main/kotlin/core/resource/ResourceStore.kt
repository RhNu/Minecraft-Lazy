package rhx.lazy.core.resource

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import rhx.lazy.integration.api.LazyInternalApi
import kotlin.math.min

/**
 * A fixed number of resource identities, each paired with a [Long] quantity.
 *
 * All mutations are planned against a private copy and committed with one notification. This keeps
 * compound machine work atomic without exposing mutable stack templates.
 */
@LazyInternalApi
public class ResourceStore<V : ResourceVariant>(
    val kind: ResourceKind<V>,
    val slots: Int,
    val amountLimit: Long = Long.MAX_VALUE,
    private val changed: () -> Unit = {},
) {
    private var entries = MutableList<Entry<V>?>(slots) { null }

    init {
        require(slots > 0) { "A resource store needs at least one slot" }
        require(amountLimit > 0L) { "A resource store needs a positive amount limit" }
    }

    val isEmpty: Boolean
        get() = entries.all { it == null }

    fun variant(slot: Int): V? = entry(slot)?.let { kind.copy(it.variant) }

    fun amount(slot: Int): Long = entry(slot)?.amount ?: 0L

    fun snapshot(): List<ResourceAmount<V>> = entries.mapNotNull { entry -> entry?.let { ResourceAmount(kind, it.variant, it.amount) } }

    fun matches(
        slot: Int,
        variant: V,
    ): Boolean = entry(slot)?.let { kind.matches(it.variant, variant) } == true

    fun capacityFor(variant: V): Long {
        var capacity = 0L
        entries.forEach { entry ->
            val addition =
                when {
                    entry == null -> amountLimit
                    kind.matches(entry.variant, variant) -> amountLimit - entry.amount
                    else -> 0L
                }
            capacity = addCapacity(capacity, addition)
        }
        return capacity
    }

    fun insert(
        amount: ResourceAmount<V>,
        simulate: Boolean = false,
    ): Long {
        require(amount.kind === kind) { "Resource amount kind ${amount.kind.id} does not match store kind ${kind.id}" }
        val state = copyEntries()
        val inserted = insertInto(state, amount.variant, amount.amount)
        if (!simulate && inserted > 0L) commit(state)
        return inserted
    }

    fun insertIntoSlot(
        slot: Int,
        amount: ResourceAmount<V>,
        simulate: Boolean = false,
    ): Long {
        validateSlot(slot)
        require(amount.kind === kind) { "Resource amount kind ${amount.kind.id} does not match store kind ${kind.id}" }
        val state = copyEntries()
        val stored = state[slot]
        if (stored != null && !kind.matches(stored.variant, amount.variant)) return 0L
        val accepted = min(amount.amount, amountLimit - (stored?.amount ?: 0L))
        if (accepted <= 0L) return 0L
        state[slot] = Entry(kind.copy(amount.variant), (stored?.amount ?: 0L) + accepted)
        if (!simulate) commit(state)
        return accepted
    }

    fun extract(
        slot: Int,
        requested: Long,
        simulate: Boolean = false,
    ): ResourceAmount<V>? {
        if (requested <= 0L) return null
        val stored = entry(slot) ?: return null
        val extracted = min(requested, stored.amount)
        if (!simulate) {
            val state = copyEntries()
            val remaining = stored.amount - extracted
            state[slot] = if (remaining == 0L) null else Entry(stored.variant, remaining)
            commit(state)
        }
        return ResourceAmount(kind, stored.variant, extracted)
    }

    internal fun tryApply(delta: ResourceDelta<V>): Boolean {
        val prepared = prepare(delta) ?: return false
        prepared.commit()
        return true
    }

    internal fun prepare(delta: ResourceDelta<V>): PreparedStoreMutation<V>? {
        val state = copyEntries()
        for (amount in delta.extracted) {
            require(amount.kind === kind) { "Extracted resource kind does not match store kind" }
            if (!extractFrom(state, amount.variant, amount.amount)) return null
        }
        for (amount in delta.inserted) {
            require(amount.kind === kind) { "Inserted resource kind does not match store kind" }
            if (insertInto(state, amount.variant, amount.amount) != amount.amount) return null
        }
        return PreparedStoreMutation(this, state)
    }

    fun clear() {
        if (isEmpty) return
        commit(MutableList(slots) { null })
    }

    /** Replaces one entry without exposing a mutable stack or partially applying the change. */
    fun replace(
        slot: Int,
        amount: ResourceAmount<V>?,
        notify: Boolean = true,
    ) {
        validateSlot(slot)
        require(amount == null || amount.kind === kind) { "Replacement resource kind does not match store kind" }
        require(amount == null || amount.amount <= amountLimit) { "Replacement amount exceeds the entry limit" }
        val replacement = amount?.let { Entry(kind.copy(it.variant), it.amount) }
        val current = entries[slot]
        if (
            current?.amount == replacement?.amount &&
            (current == null || replacement == null || kind.matches(current.variant, replacement.variant))
        ) {
            return
        }
        val state = copyEntries()
        state[slot] = replacement
        entries = state
        if (notify) changed()
    }

    fun save(registries: HolderLookup.Provider): ListTag =
        ListTag().apply {
            entries.forEachIndexed { slot, entry ->
                if (entry == null) return@forEachIndexed
                add(
                    CompoundTag().apply {
                        putInt(SLOT_TAG, slot)
                        put(VARIANT_TAG, kind.save(registries, entry.variant))
                        putLong(AMOUNT_TAG, entry.amount)
                    },
                )
            }
        }

    /** New-format only loader. Unknown, duplicate or invalid entries are ignored. */
    fun load(
        registries: HolderLookup.Provider,
        tag: ListTag,
    ) {
        val loaded = MutableList<Entry<V>?>(slots) { null }
        tag.forEach { raw ->
            if (raw !is CompoundTag) return@forEach
            val slot = raw.getInt(SLOT_TAG)
            val amount = raw.getLong(AMOUNT_TAG)
            if (slot !in 0 until slots || amount !in 1..amountLimit || loaded[slot] != null) return@forEach
            val variantTag = raw.get(VARIANT_TAG).takeIf { it?.id == Tag.TAG_COMPOUND } as? CompoundTag ?: return@forEach
            val variant = kind.parse(registries, variantTag) ?: return@forEach
            loaded[slot] = Entry(variant, amount)
        }
        entries = loaded
    }

    private fun copyEntries(): MutableList<Entry<V>?> =
        entries.mapTo(mutableListOf()) { entry -> entry?.let { Entry(kind.copy(it.variant), it.amount) } }

    private fun insertInto(
        state: MutableList<Entry<V>?>,
        variant: V,
        requested: Long,
    ): Long {
        if (requested <= 0L) return 0L
        var remaining = requested

        fun fill(slot: Int) {
            if (remaining <= 0L) return
            val stored = state[slot]
            val room = amountLimit - (stored?.amount ?: 0L)
            val accepted = min(remaining, room)
            if (accepted <= 0L) return
            state[slot] = Entry(stored?.variant ?: kind.copy(variant), (stored?.amount ?: 0L) + accepted)
            remaining -= accepted
        }

        state.indices.filter { state[it]?.let { entry -> kind.matches(entry.variant, variant) } == true }.forEach(::fill)
        state.indices.filter { state[it] == null }.forEach(::fill)
        return requested - remaining
    }

    private fun extractFrom(
        state: MutableList<Entry<V>?>,
        variant: V,
        requested: Long,
    ): Boolean {
        if (requested <= 0L) return true
        var remaining = requested
        state.indices.forEach { slot ->
            if (remaining <= 0L) return@forEach
            val stored = state[slot] ?: return@forEach
            if (!kind.matches(stored.variant, variant)) return@forEach
            val extracted = min(remaining, stored.amount)
            val left = stored.amount - extracted
            state[slot] = if (left == 0L) null else Entry(stored.variant, left)
            remaining -= extracted
        }
        return remaining == 0L
    }

    private fun entry(slot: Int): Entry<V>? {
        validateSlot(slot)
        return entries[slot]
    }

    private fun validateSlot(slot: Int) {
        if (slot !in 0 until slots) throw IndexOutOfBoundsException("Resource store slot $slot is out of range")
    }

    private fun commit(state: MutableList<Entry<V>?>) {
        entries = state
        changed()
    }

    public class PreparedStoreMutation<V : ResourceVariant> public constructor(
        private val store: ResourceStore<V>,
        private val state: MutableList<Entry<V>?>,
    ) {
        fun commit() = store.commit(state)
    }

    public data class Entry<V : ResourceVariant>(
        val variant: V,
        val amount: Long,
    )

    private companion object {
        const val SLOT_TAG = "slot"
        const val VARIANT_TAG = "variant"
        const val AMOUNT_TAG = "amount"

        fun addCapacity(
            current: Long,
            addition: Long,
        ): Long = if (addition > Long.MAX_VALUE - current) Long.MAX_VALUE else current + addition
    }
}

internal object ResourceTransaction {
    fun tryApply(vararg parts: StoreDelta<*>): Boolean {
        require(parts.map { it.storeIdentity }.distinct().size == parts.size) {
            "A resource transaction may contain each store only once"
        }
        val prepared = parts.map { it.prepare() ?: return false }
        prepared.forEach(PreparedMutation::commit)
        return true
    }
}

internal class StoreDelta<V : ResourceVariant>(
    private val store: ResourceStore<V>,
    private val delta: ResourceDelta<V>,
) {
    public val storeIdentity: Any = store

    public fun prepare(): PreparedMutation? = store.prepare(delta)?.let(::PreparedMutation)
}

internal class PreparedMutation public constructor(
    private val commitAction: () -> Unit,
) {
    constructor(mutation: ResourceStore.PreparedStoreMutation<*>) : this(mutation::commit)

    fun commit() = commitAction()
}
