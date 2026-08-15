package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import java.util.WeakHashMap

internal object SimulationRecipeReloads {
    private val synchronizedSettings = WeakHashMap<MinecraftServer, SyncedSettings>()

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

    /**
     * Adapter fingerprints can be expensive to build, so they only join the periodic sync check
     * instead of [AutomaticSimulationSettings], which is sampled on every resolve.
     */
    private fun currentSettings() = SyncedSettings(AutomaticSimulationSettings.current(), AutomaticSimulationAdapters.settingsFingerprint())

    private data class SyncedSettings(
        val settings: AutomaticSimulationSettings,
        val adapterSettings: List<Pair<ResourceLocation, Any?>>,
    )

    private const val SETTINGS_CHECK_INTERVAL = 20
}
