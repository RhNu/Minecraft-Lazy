package rhx.lazy.feature.simulation

import net.minecraft.server.MinecraftServer
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import java.util.WeakHashMap

internal object SimulationRecipeReloads {
    private val synchronizedSettings = WeakHashMap<MinecraftServer, AutomaticSettings>()

    fun onDatapackSync(event: OnDatapackSyncEvent) {
        if (event.player == null) SimulationRecipeResolver.invalidate()
        event.relevantPlayers.forEach { player ->
            val displays = SimulationRecipeResolver.automaticSimulations(player.level())
            PacketDistributor.sendToPlayer(player, AutomaticSimulationSnapshotPayload(displays))
        }
        synchronizedSettings[event.playerList.server] = currentSettings()
    }

    fun onServerTick(event: ServerTickEvent.Post) {
        if (event.server.tickCount % SETTINGS_CHECK_INTERVAL != 0) return
        val settings = currentSettings()
        if (synchronizedSettings.put(event.server, settings) == settings) return
        SimulationRecipeResolver.invalidate()
        val payload = AutomaticSimulationSnapshotPayload(SimulationRecipeResolver.automaticSimulations(event.server.overworld()))
        event.server.playerList.players
            .forEach { PacketDistributor.sendToPlayer(it, payload) }
    }

    private fun currentSettings() =
        AutomaticSettings(
            SimulationConfigs.settings.defaultDuration.get(),
            SimulationConfigs.settings.automaticMinerals.get(),
            SimulationConfigs.settings.automaticMineralDuration.get(),
            SimulationConfigs.settings.automaticMineralModPriority
                .get()
                .toList(),
            AutomaticSimulationAdapters.settingsFingerprint(),
        )

    private data class AutomaticSettings(
        val defaultDuration: Int,
        val mineralsEnabled: Boolean,
        val mineralDuration: Int,
        val mineralPriorities: List<String>,
        val adapterSettings: List<Pair<net.minecraft.resources.ResourceLocation, Any?>>,
    )

    private const val SETTINGS_CHECK_INTERVAL = 20
}
