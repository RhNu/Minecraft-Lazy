package rhx.lazy.core.ui

/** Formats the quantity drawn over a [LargeItemSlot]. */
internal fun interface LargeItemCountFormatter {
    fun format(count: Long): String
}

/**
 * Keeps item counts inside a vanilla-sized slot without hiding their order of magnitude.
 *
 * Values below one thousand stay exact. Larger values use one optional decimal below ten and the
 * familiar K/M/G/T/P/E suffixes. The renderer still scales the result to the slot, so exact
 * three-digit values and values such as `999K` cannot overflow either.
 */
internal object CompactItemCountFormatter : LargeItemCountFormatter {
    private val suffixes = charArrayOf('K', 'M', 'G', 'T', 'P', 'E')

    override fun format(count: Long): String {
        val normalized = count.coerceAtLeast(0L)
        if (normalized < THOUSAND) return normalized.toString()

        var divisor = THOUSAND
        var suffixIndex = 0
        while (suffixIndex < suffixes.lastIndex && normalized / divisor >= THOUSAND) {
            divisor *= THOUSAND
            suffixIndex++
        }

        val whole = normalized / divisor
        val suffix = suffixes[suffixIndex]
        if (whole >= 10L) return "$whole$suffix"

        val tenth = normalized % divisor / (divisor / 10L)
        return if (tenth == 0L) "$whole$suffix" else "$whole.$tenth$suffix"
    }

    private const val THOUSAND = 1_000L
}
