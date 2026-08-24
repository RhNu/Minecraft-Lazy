package rhx.lazy.integration.curios

import net.minecraft.resources.ResourceLocation
import rhx.lazy.integration.api.LazyInternalApi

/** DataGen-safe Curios slot declarations without exposing the integration entrypoint implementation. */
@LazyInternalApi
public object CuriosDataGenExports {
    public const val TELEPORTER_SLOT: String = CuriosIntegrationModule.TELEPORTER_SLOT
    public const val CONFIGURATION_CARD_SLOT: String = CuriosIntegrationModule.CONFIGURATION_CARD_SLOT

    public val teleporterSlotValidator: ResourceLocation
        get() = CuriosIntegrationModule.teleporterSlotValidator

    public val configurationCardSlotValidator: ResourceLocation
        get() = CuriosIntegrationModule.configurationCardSlotValidator
}
