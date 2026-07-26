package rhx.lazy.integration

import net.neoforged.bus.api.IEventBus
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID

internal class IntegrationManager(
    private val modules: List<IntegrationModule>,
    private val isModLoaded: (String) -> Boolean,
) {
    private val logger = LogManager.getLogger("$MOD_ID/Integrations")
    private val initialized = mutableSetOf<String>()
    private val clientInitialized = mutableSetOf<String>()

    init {
        require(modules.map(IntegrationModule::modId).distinct().size == modules.size) {
            "Integration module ids must be unique"
        }
    }

    fun initialize(modBus: IEventBus) {
        initializePhase(initialized, "common", shouldInitialize = { true }) { module ->
            module.initialize(IntegrationContext(modBus))
        }
    }

    fun initializeClient(
        modBus: IEventBus,
        gameBus: IEventBus,
    ) {
        initializePhase(clientInitialized, "client", IntegrationModule::hasClientInitialization) { module ->
            module.initializeClient(ClientIntegrationContext(modBus, gameBus))
        }
    }

    private fun initializePhase(
        completed: MutableSet<String>,
        phase: String,
        shouldInitialize: (IntegrationModule) -> Boolean,
        initialize: (IntegrationModule) -> Unit,
    ) {
        modules.forEach { module ->
            if (!shouldInitialize(module) || !isModLoaded(module.modId) || module.modId in completed) return@forEach

            try {
                initialize(module)
                completed += module.modId
                logger.info("Initialized {} integration {} phase", module.modId, phase)
            } catch (error: LinkageError) {
                throw initializationFailure(module, phase, error)
            } catch (exception: RuntimeException) {
                throw initializationFailure(module, phase, exception)
            }
        }
    }

    private fun initializationFailure(
        module: IntegrationModule,
        phase: String,
        cause: Throwable,
    ): IllegalStateException =
        IllegalStateException(
            "Failed to initialize ${module.modId} integration $phase phase",
            cause,
        )
}
