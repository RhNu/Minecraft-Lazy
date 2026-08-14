package rhx.lazy.feature.simulation

import net.neoforged.neoforge.event.OnDatapackSyncEvent

internal object SimulationRecipeReloads {
    fun onDatapackSync(event: OnDatapackSyncEvent) {
        if (event.player == null) SimulationRecipeResolver.invalidate()
    }
}
