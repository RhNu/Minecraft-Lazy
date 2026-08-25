package rhx.lazy.integration.api

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.FILE,
)
@Retention(AnnotationRetention.BINARY)
public annotation class LazyInternalApi

@LazyInternalApi
public data class IntegrationConfigContext(
    val modContainer: ModContainer,
)

@LazyInternalApi
public data class IntegrationCommonContext(
    val modContainer: ModContainer,
    val modBus: IEventBus,
    val gameBus: IEventBus,
)

@LazyInternalApi
public data class IntegrationClientContext(
    val modContainer: ModContainer,
    val modBus: IEventBus,
    val gameBus: IEventBus,
)

@LazyInternalApi
public interface CommonIntegration {
    public fun registerConfig(context: IntegrationConfigContext): Unit = Unit

    public fun install(context: IntegrationCommonContext)
}

@LazyInternalApi
public interface ClientIntegration {
    public fun install(context: IntegrationClientContext)
}

/** Mod presence snapshot shared by independently constructed distribution entrypoints. */
@LazyInternalApi
public object IntegrationModSet {
    private val installationLock = Any()

    @Volatile
    private var installedMods: Set<String>? = null

    public val loadedMods: Set<String>
        get() = checkNotNull(installedMods) { "Lazy integration mod set has not been installed" }

    public fun install(modIds: Set<String>) {
        val snapshot = modIds.toSet()
        synchronized(installationLock) {
            val current = installedMods
            check(current == null || current == snapshot) {
                "Lazy integration mod set was already installed with a different snapshot"
            }
            installedMods = snapshot
        }
    }

    public fun isLoaded(modId: String): Boolean = modId in loadedMods
}
