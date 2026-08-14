package rhx.lazy.feature.simulation

internal enum class SimulationCoreTier {
    T1,
    T2,
    T3,
    T4,
    ;

    fun speedMultiplier(): Int =
        when (this) {
            T1 -> SimulationConfigs.settings.t1SpeedMultiplier.get()
            T2 -> SimulationConfigs.settings.t2SpeedMultiplier.get()
            T3 -> SimulationConfigs.settings.t3SpeedMultiplier.get()
            T4 -> SimulationConfigs.settings.t4SpeedMultiplier.get()
        }

    fun outputMultiplier(): Int =
        when (this) {
            T1 -> SimulationConfigs.settings.t1OutputMultiplier.get()
            T2 -> SimulationConfigs.settings.t2OutputMultiplier.get()
            T3 -> SimulationConfigs.settings.t3OutputMultiplier.get()
            T4 -> SimulationConfigs.settings.t4OutputMultiplier.get()
        }
}
