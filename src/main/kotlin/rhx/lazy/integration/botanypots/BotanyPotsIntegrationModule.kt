package rhx.lazy.integration.botanypots

import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object BotanyPotsIntegrationModule : IntegrationModule {
    override val modId: String = "botanypots"

    override fun initialize(context: IntegrationContext) {
        BotanyPotsAdapter.initialize(context)
    }
}
