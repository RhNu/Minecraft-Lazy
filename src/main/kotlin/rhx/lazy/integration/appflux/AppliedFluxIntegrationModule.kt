package rhx.lazy.integration.appflux

import appeng.api.stacks.AEKey
import com.glodblock.github.appflux.common.me.key.FluxKey
import com.glodblock.github.appflux.common.me.key.type.EnergyType
import rhx.lazy.core.resource.EnergyResourceKind
import rhx.lazy.core.resource.EnergyVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule
import rhx.lazy.integration.ae2.AeStoragePayload
import rhx.lazy.integration.ae2.AeStoragePayloadAdapter
import rhx.lazy.integration.ae2.AeStoragePayloadAdapters

internal object AppliedFluxIntegrationModule : IntegrationModule {
    override val modId: String = "appflux"

    override fun initialize(context: IntegrationContext) {
        AeStoragePayloadAdapters.register(
            object : AeStoragePayloadAdapter {
                override val kind = EnergyResourceKind

                override fun convert(amount: ResourceAmount<out ResourceVariant>): AeStoragePayload? {
                    if (amount.variant !is EnergyVariant) return null
                    val key: AEKey = FluxKey.of(EnergyType.FE) ?: return null
                    return AeStoragePayload(key, amount.amount)
                }
            },
        )
    }
}
