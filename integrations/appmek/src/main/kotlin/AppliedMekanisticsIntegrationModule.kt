package rhx.lazy.integration.appmek

import me.ramidzkh.mekae2.ae2.MekanismKey
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.ae2.AeStoragePayload
import rhx.lazy.integration.ae2.AeStoragePayloadAdapter
import rhx.lazy.integration.ae2.AeStoragePayloadAdapters
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext
import rhx.lazy.integration.mekanism.ChemicalResourceKind
import rhx.lazy.integration.mekanism.ChemicalVariant

@LazyCommonEntrypoint
internal object AppliedMekanisticsIntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        AeStoragePayloadAdapters.register(AppliedMekanisticsChemicalStorageAdapter)
    }
}

internal object AppliedMekanisticsChemicalStorageAdapter : AeStoragePayloadAdapter {
    override val kind = ChemicalResourceKind

    override fun convert(amount: ResourceAmount<out ResourceVariant>): AeStoragePayload? {
        val variant = amount.variant as? ChemicalVariant ?: return null
        val key = MekanismKey.of(variant.template) ?: return null
        return AeStoragePayload(key, amount.amount)
    }
}
