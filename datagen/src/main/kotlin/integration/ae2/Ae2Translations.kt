package rhx.lazy.integration.ae2

import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.core.datagen.LanguageContributions
import rhx.lazy.integration.annotation.LazyDataGenContribution

@LazyDataGenContribution(integrationId = "ae2")
internal object Ae2Translations {
    fun gatherData(event: GatherDataEvent) {
        add("gui.lazy.io.provider.ae2", "AE2 ME Network", "AE2 ME 网络")
    }

    private fun add(
        key: String,
        english: String,
        chinese: String,
    ) {
        LanguageContributions.register(key, english, chinese)
    }
}
