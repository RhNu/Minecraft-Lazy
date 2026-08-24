package rhx.lazy.integration.silentgear

import rhx.lazy.feature.repairer.ItemRepairHooks
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object SilentGearIntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        ItemRepairHooks.register(SilentGearRepairHook)
    }
}
