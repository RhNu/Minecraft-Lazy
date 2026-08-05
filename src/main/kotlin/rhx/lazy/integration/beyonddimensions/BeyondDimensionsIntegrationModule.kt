package rhx.lazy.integration.beyonddimensions

import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object BeyondDimensionsIntegrationModule : IntegrationModule {
    override val modId: String = "beyonddimensions"

    override fun initialize(context: IntegrationContext) {
        val storage =
            GuardedNetworkStoragePort(
                modId = modId,
                delegate = BeyondDimensionsStorageAdapter,
            )
        NetworkOutputProviders.register(BeyondDimensionsNetworkProvider(storage))
    }
}
