package rhx.lazy.integration.curios

import rhx.lazy.core.lazyId
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object CuriosIntegrationModule : CommonIntegration {
    const val TELEPORTER_SLOT = "teleporter"
    const val TELEPORTER_SLOT_INDEX = 0

    val teleporterSlotValidator = lazyId("teleporter_slot")

    const val CONFIGURATION_CARD_SLOT = "configuration_card"
    const val CONFIGURATION_CARD_SLOT_INDEX = 0

    val configurationCardSlotValidator = lazyId("configuration_card_slot")

    override fun install(context: IntegrationCommonContext) {
        CuriosAdapter.install(context)
    }
}
