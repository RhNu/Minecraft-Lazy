package rhx.lazy.integration

import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.integration.annotation.LazyDataGenContribution

/** Explicit ownership markers for translations currently emitted by the shared language providers. */
@LazyDataGenContribution(integrationId = "beyonddimensions")
internal object BeyondDimensionsEmbeddedTranslations {
    @Suppress("UNUSED_PARAMETER")
    fun gatherData(event: GatherDataEvent): Unit = Unit
}

@LazyDataGenContribution(integrationId = "jade")
internal object JadeEmbeddedTranslations {
    @Suppress("UNUSED_PARAMETER")
    fun gatherData(event: GatherDataEvent): Unit = Unit
}

@LazyDataGenContribution(integrationId = "jei")
internal object JeiEmbeddedTranslations {
    @Suppress("UNUSED_PARAMETER")
    fun gatherData(event: GatherDataEvent): Unit = Unit
}

@LazyDataGenContribution(integrationId = "mekanism")
internal object MekanismEmbeddedTranslations {
    @Suppress("UNUSED_PARAMETER")
    fun gatherData(event: GatherDataEvent): Unit = Unit
}
