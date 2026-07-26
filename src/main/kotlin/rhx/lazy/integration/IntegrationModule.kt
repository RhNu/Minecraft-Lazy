package rhx.lazy.integration

import net.neoforged.bus.api.IEventBus

internal data class IntegrationContext(
    val modBus: IEventBus,
)

internal data class ClientIntegrationContext(
    val modBus: IEventBus,
    val gameBus: IEventBus,
)

internal interface IntegrationModule {
    val modId: String

    val hasClientInitialization: Boolean
        get() = false

    fun initialize(context: IntegrationContext)

    fun initializeClient(context: ClientIntegrationContext) = Unit
}
