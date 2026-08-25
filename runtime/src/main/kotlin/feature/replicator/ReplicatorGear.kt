package rhx.lazy.feature.replicator

import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public enum class ReplicatorGear(
    val intervalTicks: Int,
) {
    FAST(10),
    NORMAL(20),
    SLOW(100),
    VERY_SLOW(200),
    ;

    internal fun next(): ReplicatorGear {
        val gears = entries
        return gears[(ordinal + 1) % gears.size]
    }

    companion object {
        internal val DEFAULT = NORMAL

        internal fun fromInterval(intervalTicks: Int): ReplicatorGear = entries.firstOrNull { it.intervalTicks == intervalTicks } ?: DEFAULT
    }
}
