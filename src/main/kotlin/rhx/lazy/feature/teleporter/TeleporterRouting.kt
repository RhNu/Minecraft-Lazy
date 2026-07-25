package rhx.lazy.feature.teleporter

internal data class TeleporterRoute(
    val requestedDestination: SavedLocation,
    private val currentLocation: SavedLocation,
    private val oldData: TeleporterData,
    private val leavingVoid: Boolean,
) {
    fun completed(actualDestination: SavedLocation): TeleporterData =
        if (leavingVoid) {
            oldData.copy(targetLocation = currentLocation)
        } else {
            TeleporterData(currentLocation, actualDestination)
        }
}

internal object TeleporterRouting {
    fun select(
        leavingVoid: Boolean,
        currentLocation: SavedLocation,
        oldData: TeleporterData,
        defaultReturn: SavedLocation,
        defaultTarget: SavedLocation,
    ): TeleporterRoute =
        TeleporterRoute(
            requestedDestination =
                if (leavingVoid) {
                    oldData.returnLocation ?: defaultReturn
                } else {
                    oldData.targetLocation ?: defaultTarget
                },
            currentLocation = currentLocation,
            oldData = oldData,
            leavingVoid = leavingVoid,
        )
}
