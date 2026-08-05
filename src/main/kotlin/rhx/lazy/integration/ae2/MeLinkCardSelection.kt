package rhx.lazy.integration.ae2

internal sealed interface MeLinkCardSelection<out T> {
    data class Selected<T>(
        val target: T,
    ) : MeLinkCardSelection<T>

    data object Missing : MeLinkCardSelection<Nothing>

    data object Ambiguous : MeLinkCardSelection<Nothing>

    companion object {
        fun <T> select(
            handTargets: List<T?>,
            inventoryTargets: List<T>,
        ): MeLinkCardSelection<T> {
            handTargets.firstNotNullOfOrNull { it }?.let { return Selected(it) }
            val distinctTargets = inventoryTargets.distinct()
            return when (distinctTargets.size) {
                0 -> Missing
                1 -> Selected(distinctTargets.single())
                else -> Ambiguous
            }
        }
    }
}
