package rhx.lazy.integration.curios

import rhx.lazy.core.lazyId
import rhx.lazy.integration.ClientIntegrationContext
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule
import rhx.lazy.integration.curios.client.CuriosClientAdapter

internal object CuriosIntegrationModule : IntegrationModule {
    const val TELEPORTER_SLOT = "teleporter"
    const val TELEPORTER_SLOT_INDEX = 0

    val teleporterSlotValidator = lazyId("teleporter_slot")

    const val ME_LINK_CARD_SLOT = "me_link_card"
    const val ME_LINK_CARD_SLOT_INDEX = 0

    val meLinkCardSlotValidator = lazyId("me_link_card_slot")

    override val modId: String = "curios"
    override val hasClientInitialization: Boolean = true

    override fun initialize(context: IntegrationContext) {
        CuriosAdapter.initialize(context)
    }

    override fun initializeClient(context: ClientIntegrationContext) {
        CuriosClientAdapter.initialize(context)
    }
}
