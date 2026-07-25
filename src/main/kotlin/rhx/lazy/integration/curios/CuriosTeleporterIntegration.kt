package rhx.lazy.integration.curios

import rhx.lazy.core.lazyId
import rhx.lazy.feature.teleporter.TeleporterRegistries
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosTeleporterIntegration {
    const val TELEPORTER_SLOT = "teleporter"
    const val TELEPORTER_SLOT_INDEX = 0
    val teleporterSlotValidator = lazyId("teleporter_slot")

    fun registerPredicates() {
        CuriosApi.registerCurioPredicate(teleporterSlotValidator) { result ->
            result.stack().item === TeleporterRegistries.item.get()
        }
    }
}
