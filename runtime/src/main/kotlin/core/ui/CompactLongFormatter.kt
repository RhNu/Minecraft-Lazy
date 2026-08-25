package rhx.lazy.core.ui

import rhx.lazy.integration.api.LazyInternalApi

/**
 * Formats a signed [Long] with the familiar K/M/G/T/P/E metric suffixes.
 *
 * Values below one thousand stay exact. Compact values retain one decimal digit while their whole
 * part is below ten, then use whole units. The implementation uses integer arithmetic so it is
 * locale-independent, allocation-light, and safe for both [Long.MIN_VALUE] and [Long.MAX_VALUE].
 */
@LazyInternalApi
public object CompactLongFormatter {
    public fun format(value: Long): String {
        if (value in -999L..999L) return value.toString()

        val negative = value < 0L
        val negativeMagnitude = if (negative) value else -value
        var divisor = THOUSAND
        var suffixIndex = 0
        while (suffixIndex < SUFFIXES.lastIndex && negativeMagnitude / divisor <= -THOUSAND) {
            divisor *= THOUSAND
            suffixIndex++
        }

        val whole = -(negativeMagnitude / divisor)
        val tenth = -(negativeMagnitude % divisor) / (divisor / 10L)
        return buildString(MAX_RESULT_LENGTH) {
            if (negative) append('-')
            append(whole)
            if (whole < 10L && tenth != 0L) {
                append('.')
                append(tenth)
            }
            append(SUFFIXES[suffixIndex])
        }
    }

    private const val SUFFIXES = "KMGTPE"
    private const val THOUSAND = 1_000L
    private const val MAX_RESULT_LENGTH = 8
}
