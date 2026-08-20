package rhx.lazy.integration.mekanism

import rhx.lazy.core.configurator.ModularConfiguratorModules
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object MekanismIntegrationModule : IntegrationModule {
    override val modId: String = "mekanism"

    override fun initialize(context: IntegrationContext) {
        ModularConfiguratorModules.register(MekanismConfiguratorModule)
    }
}
