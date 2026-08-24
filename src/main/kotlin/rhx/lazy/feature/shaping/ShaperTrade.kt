package rhx.lazy.feature.shaping

import kotlin.math.min

/**
 * The smallest whole-item exchange between two material forms.
 *
 * Reducing by the greatest common divisor is what makes the shaper lossless. Nine nuggets buy one
 * ingot, eight rods buy one gear, one gear buys four plates — and because the machine only ever runs
 * whole trades, material that cannot fill one stays in the input slot where the player can see it.
 * There is no remainder ledger to persist, and nothing to lose when the machine is broken.
 */
internal data class ShaperTrade(
    val inputPerTrade: Int,
    val outputPerTrade: Int,
) {
    init {
        require(inputPerTrade > 0 && outputPerTrade > 0) { "A shaper trade must move at least one item each way" }
    }

    /**
     * How many whole trades fit, given what the lane holds and what the output pool can still take.
     * Both limits are floors, so a partly-full output never causes a partial trade.
     */
    fun trades(
        available: Int,
        capacity: Int,
    ): Int {
        if (available < inputPerTrade || capacity < outputPerTrade) return 0
        return min(available / inputPerTrade, capacity / outputPerTrade)
    }
}

/** Null when either side has no sensible unit value, which a bad datapack entry could produce. */
internal fun shaperTrade(
    inputUnits: Int,
    outputUnits: Int,
): ShaperTrade? {
    if (inputUnits <= 0 || outputUnits <= 0) return null
    val divisor = greatestCommonDivisor(inputUnits, outputUnits)
    return ShaperTrade(outputUnits / divisor, inputUnits / divisor)
}

private tailrec fun greatestCommonDivisor(
    first: Int,
    second: Int,
): Int = if (second == 0) first else greatestCommonDivisor(second, first % second)
