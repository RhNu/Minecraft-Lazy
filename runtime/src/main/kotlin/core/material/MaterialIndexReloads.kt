package rhx.lazy.core.material

import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.event.TagsUpdatedEvent

/**
 * Keeps the material index in step with the two things it is derived from.
 *
 * Tags and the priority setting arrive over different channels and their order cannot be assumed:
 * tags come with vanilla's `SynchronizeRegistriesTask`, while NeoForge's `SyncConfig` is an ordinary
 * configuration task registered afterwards, so a client normally sees tags first and the config
 * second. Rather than depend on that, both signals invalidate, and whichever lands last wins.
 *
 * [TagsUpdatedEvent.shouldUpdateStaticData] decides who owns the cache in each setup: the server
 * builds it on a dedicated server and in single player, and the client builds it only when it is
 * talking to a remote server. That way exactly one side per JVM populates the cache, and the
 * single-player client never fights the integrated server over it.
 */
internal object MaterialIndexReloads {
    fun onTagsUpdated(event: TagsUpdatedEvent) {
        if (!event.shouldUpdateStaticData()) return
        MaterialIndexes.refresh(event.registryAccess)
    }

    fun onConfigLoading(event: ModConfigEvent.Loading) {
        invalidateIfOwned(event)
    }

    fun onConfigReloading(event: ModConfigEvent.Reloading) {
        invalidateIfOwned(event)
    }

    private fun invalidateIfOwned(event: ModConfigEvent) {
        if (event.config.fileName != MaterialConfigs.FILE_NAME) return
        MaterialIndexes.rebuild()
    }
}
