package rhx.lazy.integration.curios

import net.neoforged.fml.ModList
import rhx.lazy.integration.ae2.Ae2Registries
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosMeLinkCardSlot {
    private const val AE2_MOD_ID = "ae2"

    fun isAvailable(): Boolean = ModList.get().isLoaded(AE2_MOD_ID)

    fun registerValidator() {
        CuriosApi.registerCurioPredicate(CuriosIntegrationModule.meLinkCardSlotValidator) { result ->
            result.stack().item === Ae2Registries.meOutputLinkCard.get()
        }
    }
}
