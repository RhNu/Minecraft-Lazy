package rhx.lazy.integration.mysticalagriculture

import kotlin.math.min

internal data class EssenceLedger(
    val target: EssenceTier? = null,
    val outputCount: Long = 0L,
    val remainderUnits: Int = 0,
) {
    val hasContents: Boolean
        get() = outputCount > 0L || remainderUnits > 0

    fun withTarget(newTarget: EssenceTier): EssenceLedger? =
        if (hasContents && target != newTarget) {
            null
        } else {
            copy(target = newTarget)
        }

    fun insert(
        tier: EssenceTier,
        requested: Int,
        capacity: Long,
    ): EssenceInsertion {
        val selected = target ?: return EssenceInsertion(this, 0)
        if (requested <= 0 || capacity <= 0L || outputCount < 0L || outputCount > capacity) {
            return EssenceInsertion(this, 0)
        }

        val targetValue = selected.inferiumValue.toLong()
        val inputValue = tier.inferiumValue.toLong()
        val safeRemainder = remainderUnits.coerceIn(0, selected.inferiumValue - 1).toLong()
        val availableOutputs = capacity - outputCount
        val accepted =
            if (availableOutputs > Long.MAX_VALUE / targetValue) {
                requested
            } else {
                val remainingValue = availableOutputs * targetValue - safeRemainder
                min(requested.toLong(), remainingValue.coerceAtLeast(0L) / inputValue).toInt()
            }
        if (accepted <= 0) return EssenceInsertion(this, 0)

        val combinedValue = safeRemainder + accepted.toLong() * inputValue
        val produced = combinedValue / targetValue
        val remainder = (combinedValue % targetValue).toInt()
        return EssenceInsertion(
            EssenceLedger(selected, outputCount + produced, remainder),
            accepted,
        )
    }

    fun extract(
        requested: Int,
        limit: Int,
    ): EssenceExtraction {
        if (requested <= 0 || limit <= 0 || outputCount <= 0L) return EssenceExtraction(this, 0)
        val extracted = min(min(requested, limit).toLong(), outputCount).toInt()
        return EssenceExtraction(copy(outputCount = outputCount - extracted), extracted)
    }

    fun removeOutput(amount: Long): EssenceLedger {
        if (amount <= 0L || outputCount <= 0L) return this
        return copy(outputCount = (outputCount - amount).coerceAtLeast(0L))
    }

    fun clear(): EssenceLedger = copy(outputCount = 0L, remainderUnits = 0)

    fun normalize(capacity: Long): EssenceLedger {
        val selected = target ?: return EssenceLedger()
        val safeCapacity = capacity.coerceAtLeast(1L)
        val count = outputCount.coerceIn(0L, safeCapacity)
        val remainder =
            if (count == safeCapacity) {
                0
            } else {
                remainderUnits.coerceIn(0, selected.inferiumValue - 1)
            }
        return EssenceLedger(selected, count, remainder)
    }

    fun downgradeMissingInsanium(capacity: Long): EssenceLedger {
        if (target != EssenceTier.INSANIUM || EssenceTier.INSANIUM.isAvailable()) {
            return normalize(capacity)
        }

        val safeCount = outputCount.coerceAtLeast(0L)
        val safeRemainder = remainderUnits.coerceIn(0, EssenceTier.INSANIUM.inferiumValue - 1)
        val extraSupremium = safeRemainder / EssenceTier.SUPREMIUM.inferiumValue
        val convertedCount = saturatedMultiplyAdd(safeCount, 4L, extraSupremium.toLong())
        return EssenceLedger(
            target = EssenceTier.SUPREMIUM,
            outputCount = convertedCount,
            remainderUnits = safeRemainder % EssenceTier.SUPREMIUM.inferiumValue,
        ).normalize(capacity)
    }
}

internal data class EssenceInsertion(
    val ledger: EssenceLedger,
    val accepted: Int,
)

internal data class EssenceExtraction(
    val ledger: EssenceLedger,
    val extracted: Int,
)

private fun saturatedMultiplyAdd(
    value: Long,
    multiplier: Long,
    addition: Long,
): Long {
    if (value > (Long.MAX_VALUE - addition) / multiplier) return Long.MAX_VALUE
    return value * multiplier + addition
}
