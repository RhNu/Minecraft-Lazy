package rhx.lazy.integration.mysticalagriculture

internal enum class EssenceOutputMode {
    DOWNWARD,
    NETWORK,
}

internal data class EssenceOutputState(
    val mode: EssenceOutputMode,
    val networkId: Int,
    val networkPaused: Boolean,
) {
    fun repairAfterLoad(networkAvailable: Boolean): EssenceOutputState =
        if (mode == EssenceOutputMode.NETWORK && networkId >= 0 && networkAvailable) {
            this
        } else {
            downward()
        }

    companion object {
        fun downward(): EssenceOutputState =
            EssenceOutputState(
                mode = EssenceOutputMode.DOWNWARD,
                networkId = -1,
                networkPaused = false,
            )
    }
}
