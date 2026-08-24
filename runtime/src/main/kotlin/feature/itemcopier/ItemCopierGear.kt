package rhx.lazy.feature.itemcopier

import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public enum class ItemCopierGear(
    val intervalTicks: Int,
) {
    FAST(10),
    NORMAL(20),
    SLOW(100),
    VERY_SLOW(200),
    ;

    fun next(): ItemCopierGear {
        val gears = entries
        return gears[(ordinal + 1) % gears.size]
    }

    companion object {
        val DEFAULT = NORMAL

        fun fromInterval(intervalTicks: Int): ItemCopierGear = entries.firstOrNull { it.intervalTicks == intervalTicks } ?: DEFAULT
    }
}
