package rhx.lazy.integration.beyonddimensions

import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.storage.NetworkStorage
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
        NetworkStorage.install(storage)
        NetworkOutputProviders.register(BeyondDimensionsNetworkProvider(storage))
    }
}
