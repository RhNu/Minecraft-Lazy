package rhx.lazy.integration.mekanism

import rhx.lazy.core.configurator.ModularConfiguratorModules
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object MekanismIntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        ModularConfiguratorModules.register(MekanismConfiguratorModule)
    }
}
