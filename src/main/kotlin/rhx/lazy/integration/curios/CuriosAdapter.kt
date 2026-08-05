package rhx.lazy.integration.curios

import rhx.lazy.feature.teleporter.TeleporterRegistries
import rhx.lazy.integration.IntegrationContext
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosAdapter {
    fun initialize(context: IntegrationContext) {
        CuriosApi.registerCurioPredicate(CuriosIntegrationModule.teleporterSlotValidator) { result ->
            result.stack().item === TeleporterRegistries.item.get()
        }
        if (CuriosMeLinkCardSlot.isAvailable()) {
            CuriosMeLinkCardSlot.registerValidator()
        }
        context.modBus.addListener(CuriosTeleporterNetworking::register)
        context.modBus.addListener(CuriosDataGeneration::gatherData)
    }
}
