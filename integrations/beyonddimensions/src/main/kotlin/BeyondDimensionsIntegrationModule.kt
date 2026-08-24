package rhx.lazy.integration.beyonddimensions

import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object BeyondDimensionsIntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        val storage =
            GuardedNetworkStoragePort(
                modId = "beyonddimensions",
                delegate = BeyondDimensionsStorageAdapter,
            )
        NetworkOutputProviders.register(BeyondDimensionsNetworkProvider(storage))
    }
}
