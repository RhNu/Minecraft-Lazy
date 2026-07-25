package rhx.lazy.curios

import rhx.lazy.registry.ModItems
import rhx.lazy.util.lazyId
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosIntegration {
    const val TELEPORTER_SLOT = "teleporter"
    const val TELEPORTER_SLOT_INDEX = 0
    val teleporterSlotValidator = lazyId("teleporter_slot")

    fun registerPredicates() {
        CuriosApi.registerCurioPredicate(teleporterSlotValidator) { result ->
            result.stack().item === ModItems.teleporter.get()
        }
    }
}
