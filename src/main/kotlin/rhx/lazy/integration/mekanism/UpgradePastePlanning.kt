package rhx.lazy.integration.mekanism

internal enum class UpgradePasteStatus {
    ALREADY_SATISFIED,
    COMPLETE,
    PARTIAL,
    NONE_INSTALLED,
}

internal data class UpgradePastePlan<T>(
    val required: Map<T, Int>,
) {
    fun status(installed: Map<T, Int>): UpgradePasteStatus {
        if (required.isEmpty()) return UpgradePasteStatus.ALREADY_SATISFIED
        val requiredTotal = required.values.sum()
        val installedTotal = installed.values.sum()
        return when {
            installedTotal <= 0 -> UpgradePasteStatus.NONE_INSTALLED
            installedTotal >= requiredTotal -> UpgradePasteStatus.COMPLETE
            else -> UpgradePasteStatus.PARTIAL
        }
    }

    fun missing(installed: Map<T, Int>): Map<T, Int> =
        required
            .mapNotNull { (type, amount) ->
                (amount - installed.getOrDefault(type, 0)).takeIf { missing -> missing > 0 }?.let { type to it }
            }.toMap(LinkedHashMap())

    companion object {
        fun <T> create(
            orderedTypes: Iterable<T>,
            desired: Map<T, Int>,
            current: Map<T, Int>,
        ): UpgradePastePlan<T> =
            UpgradePastePlan(
                buildMap {
                    orderedTypes.forEach { type ->
                        val deficit = desired.getOrDefault(type, 0) - current.getOrDefault(type, 0)
                        if (deficit > 0) put(type, deficit)
                    }
                },
            )
    }
}
