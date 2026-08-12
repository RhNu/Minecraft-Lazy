package rhx.lazy.core.storage

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import kotlin.math.min

/**
 * An item and component template paired with a count that is not limited by [ItemStack].
 * The template is always stored with a count of one and is never exposed directly.
 */
internal class LongItemStack(
    stack: ItemStack,
    val count: Long,
) {
    private val storedTemplate = stack.copyWithCount(1)

    init {
        require(!stack.isEmpty) { "A long item stack requires a non-empty template" }
        require(count > 0L) { "A long item stack requires a positive count" }
    }

    val template: ItemStack
        get() = storedTemplate.copy()

    fun matches(stack: ItemStack): Boolean = !stack.isEmpty && ItemStack.isSameItemSameComponents(storedTemplate, stack)

    fun withCount(newCount: Long): LongItemStack {
        require(newCount > 0L) { "A long item stack requires a positive count" }
        return LongItemStack(storedTemplate, newCount)
    }

    fun plus(addition: Long): LongItemStack {
        require(addition >= 0L) { "Can not add a negative item count" }
        val total =
            if (addition > Long.MAX_VALUE - count) {
                Long.MAX_VALUE
            } else {
                count + addition
            }
        return withCount(total)
    }

    fun toItemStacks(): Sequence<ItemStack> =
        sequence {
            var remaining = count
            val maxStackSize = storedTemplate.maxStackSize.coerceAtLeast(1).toLong()
            while (remaining > 0L) {
                val amount = min(remaining, maxStackSize).toInt()
                yield(storedTemplate.copyWithCount(amount))
                remaining -= amount
            }
        }

    fun save(registries: HolderLookup.Provider): CompoundTag =
        CompoundTag().apply {
            put(STACK_TAG, storedTemplate.save(registries))
            putLong(COUNT_TAG, count)
        }

    companion object {
        private const val STACK_TAG = "stack"
        private const val COUNT_TAG = "count"

        fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): LongItemStack? {
            val stack = ItemStack.parseOptional(registries, tag.getCompound(STACK_TAG))
            val count = tag.getLong(COUNT_TAG)
            return if (stack.isEmpty || count <= 0L) null else LongItemStack(stack, count)
        }
    }
}
