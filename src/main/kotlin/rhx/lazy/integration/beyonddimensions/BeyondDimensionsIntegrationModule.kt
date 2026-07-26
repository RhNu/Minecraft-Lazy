package rhx.lazy.integration.beyonddimensions

import rhx.lazy.core.storage.NetworkStorage
import rhx.lazy.integration.IntegrationContext
import rhx.lazy.integration.IntegrationModule

internal object BeyondDimensionsIntegrationModule : IntegrationModule {
    override val modId: String = "beyonddimensions"

    override fun initialize(context: IntegrationContext) {
        NetworkStorage.install(
            GuardedNetworkStoragePort(
                modId = modId,
                delegate = BeyondDimensionsStorageAdapter,
            ),
        )
    }
}
