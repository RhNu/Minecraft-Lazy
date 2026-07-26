package rhx.lazy.integration.silentgear

import rhx.lazy.feature.repairer.ItemRepairHooks
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object SilentGearIntegrationModule : IntegrationModule {
    override val modId: String = "silentgear"

    override fun initialize(context: IntegrationContext) {
        ItemRepairHooks.register(SilentGearRepairHook)
    }
}
