package rhx.lazy.integration.botanypots

import rhx.lazy.integration.ClientIntegrationContext
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule
import rhx.lazy.integration.botanypots.client.BotanyPotsClientAdapter

internal object BotanyPotsIntegrationModule : IntegrationModule {
    override val modId: String = "botanypots"
    override val hasClientInitialization: Boolean = true

    override fun initialize(context: IntegrationContext) {
        BotanyPotsAdapter.initialize(context)
    }

    override fun initializeClient(context: ClientIntegrationContext) {
        BotanyPotsClientAdapter.initialize(context)
    }
}
