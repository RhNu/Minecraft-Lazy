package rhx.lazy.integration.curios

import rhx.lazy.core.io.ConfigurationCardSources
import rhx.lazy.feature.teleporter.TeleporterRegistries
import rhx.lazy.integration.api.IntegrationCommonContext
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosAdapter {
    fun install(context: IntegrationCommonContext) {
        CuriosApi.registerCurioPredicate(CuriosIntegrationModule.teleporterSlotValidator) { result ->
            result.stack().item === TeleporterRegistries.item.get()
        }
        CuriosConfigurationCardSource.registerValidator()
        ConfigurationCardSources.register(CuriosConfigurationCardSource)
        context.modBus.addListener(CuriosTeleporterNetworking::register)
    }
}
