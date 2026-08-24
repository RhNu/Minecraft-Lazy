package rhx.lazy.feature.shaping

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandlerModifiable
import kotlin.math.min

/**
 * A pool of large single-type slots, stored as a template plus a count.
 *
 * Splitting the count out of the [ItemStack] is how the buffer already holds more than a stack per
 * slot, and the shaper needs the same: one lane holds 1024 pieces so that a wall of machines is
 * bounded by eight materials each, not by how many slots someone filled.
 */
internal class ShaperLanes(
    private val templates: MutableList<ItemStack>,
    private val counts: MutableList<Int>,
    private val laneCount: Int,
    private val capacity: Int,
    private val changed: () -> Unit,
) {
    val size: Int
        get() = laneCount

    val isEmpty: Boolean
        get() = counts.all { it <= 0 }

    fun template(lane: Int): ItemStack = templates[lane]

    fun count(lane: Int): Int = counts[lane]

    fun stackInLane(lane: Int): ItemStack {
        val template = templates[lane]
        val count = counts[lane]
        return if (template.isEmpty || count <= 0) ItemStack.EMPTY else template.copyWithCount(count)
    }

    fun take(
        lane: Int,
        amount: Int,
    ) {
        if (amount <= 0) return
        val remaining = (counts[lane] - amount).coerceAtLeast(0)
        counts[lane] = remaining
        if (remaining == 0) templates[lane] = ItemStack.EMPTY
        changed()
    }

    /** Total room across every lane that already holds [stack] plus every empty lane. */
    fun capacityFor(stack: ItemStack): Int {
        if (stack.isEmpty) return 0
        var room = 0
        for (lane in templates.indices) {
            val template = templates[lane]
            room +=
                when {
                    template.isEmpty || counts[lane] <= 0 -> capacity
                    ItemStack.isSameItemSameComponents(template, stack) -> capacity - counts[lane]
                    else -> 0
                }
        }
        return room
    }

    /** Fills matching lanes before empty ones. Returns how much was placed. */
    fun insert(
        stack: ItemStack,
        amount: Int,
    ): Int {
        if (stack.isEmpty || amount <= 0) return 0
        var remaining = amount
        remaining -= fill(stack, remaining) { lane -> !templates[lane].isEmpty && counts[lane] > 0 }
        remaining -= fill(stack, remaining) { lane -> templates[lane].isEmpty || counts[lane] <= 0 }
        val inserted = amount - remaining
        if (inserted > 0) changed()
        return inserted
    }

    fun accepts(
        lane: Int,
        stack: ItemStack,
    ): Boolean {
        val template = templates[lane]
        return template.isEmpty || counts[lane] <= 0 || ItemStack.isSameItemSameComponents(template, stack)
    }

    fun set(
        lane: Int,
        stack: ItemStack,
    ) {
        if (stack.isEmpty) {
            templates[lane] = ItemStack.EMPTY
            counts[lane] = 0
        } else {
            templates[lane] = stack.copyWithCount(1)
            counts[lane] = stack.count.coerceIn(1, capacity)
        }
        changed()
    }

    fun slotLimit(): Int = capacity

    fun normalize() {
        templates.resize(laneCount) { ItemStack.EMPTY }
        counts.resize(laneCount) { 0 }
        for (lane in 0 until laneCount) {
            val template = templates[lane]
            val count = counts[lane].coerceIn(0, capacity)
            if (template.isEmpty || count == 0) {
                templates[lane] = ItemStack.EMPTY
                counts[lane] = 0
            } else {
                templates[lane] = template.copyWithCount(1)
                counts[lane] = count
            }
        }
    }

    private inline fun fill(
        stack: ItemStack,
        amount: Int,
        predicate: (Int) -> Boolean,
    ): Int {
        if (amount <= 0) return 0
        var remaining = amount
        for (lane in templates.indices) {
            if (remaining <= 0) break
            if (!predicate(lane)) continue
            val stored = if (templates[lane].isEmpty) 0 else counts[lane]
            if (stored > 0 && !ItemStack.isSameItemSameComponents(templates[lane], stack)) continue
            val accepted = min(remaining, capacity - stored)
            if (accepted <= 0) continue
            templates[lane] = stack.copyWithCount(1)
            counts[lane] = stored + accepted
            remaining -= accepted
        }
        return amount - remaining
    }
}

/**
 * Capability view over a [ShaperLanes] pool.
 *
 * Extraction is clamped to the item's own maximum stack size even though a lane holds far more, so a
 * hopper or pipe never receives a stack it cannot represent.
 */
internal class ShaperLaneHandler(
    private val lanes: ShaperLanes,
    private val allowInsert: Boolean,
    private val isValid: (ItemStack) -> Boolean,
) : IItemHandlerModifiable {
    override fun getSlots(): Int = lanes.size

    override fun getStackInSlot(slot: Int): ItemStack {
        validate(slot)
        return lanes.stackInLane(slot)
    }

    override fun insertItem(
        slot: Int,
        stack: ItemStack,
        simulate: Boolean,
    ): ItemStack {
        validate(slot)
        if (stack.isEmpty || !isItemValid(slot, stack)) return stack
        val stored = if (lanes.template(slot).isEmpty) 0 else lanes.count(slot)
        val accepted = min(stack.count, lanes.slotLimit() - stored)
        if (accepted <= 0) return stack
        if (!simulate) lanes.set(slot, stack.copyWithCount(stored + accepted))
        return if (accepted == stack.count) ItemStack.EMPTY else stack.copyWithCount(stack.count - accepted)
    }

    override fun extractItem(
        slot: Int,
        amount: Int,
        simulate: Boolean,
    ): ItemStack {
        validate(slot)
        val template = lanes.template(slot)
        val stored = lanes.count(slot)
        if (amount <= 0 || template.isEmpty || stored <= 0) return ItemStack.EMPTY
        val extracted = min(amount, min(stored, template.maxStackSize.coerceAtLeast(1)))
        if (!simulate) lanes.take(slot, extracted)
        return template.copyWithCount(extracted)
    }

    override fun getSlotLimit(slot: Int): Int {
        validate(slot)
        return lanes.slotLimit()
    }

    override fun isItemValid(
        slot: Int,
        stack: ItemStack,
    ): Boolean {
        validate(slot)
        return allowInsert && !stack.isEmpty && lanes.accepts(slot, stack) && isValid(stack)
    }

    override fun setStackInSlot(
        slot: Int,
        stack: ItemStack,
    ) {
        validate(slot)
        if (stack.isEmpty || isValid(stack)) lanes.set(slot, stack)
    }

    private fun validate(slot: Int) {
        if (slot !in 0 until lanes.size) throw IndexOutOfBoundsException("Shaper lane $slot is out of range")
    }
}

internal fun <T> MutableList<T>.resize(
    size: Int,
    factory: () -> T,
) {
    while (this.size > size) removeAt(lastIndex)
    while (this.size < size) add(factory())
}
