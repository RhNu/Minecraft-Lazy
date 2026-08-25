package rhx.lazy.core.ui

/** Formats the quantity drawn over a [LargeItemSlot]. */
internal fun interface LargeItemCountFormatter {
    fun format(count: Long): String
}

internal val compactItemCountFormatter =
    LargeItemCountFormatter { count ->
        CompactLongFormatter.format(count.coerceAtLeast(0L))
    }
