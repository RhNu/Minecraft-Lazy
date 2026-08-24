package rhx.lazy.buildlogic

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

public enum class IntegrationOwner {
    LAZY,
    JADE,
    JEI,
    KUBEJS,
}

public enum class IntegrationSide {
    BOTH,
    CLIENT,
}

public abstract class LazyIntegrationExtension @Inject constructor(
    objects: ObjectFactory,
) {
    public val id: Property<String> = objects.property(String::class.java)
    public val owner: Property<IntegrationOwner> = objects.property(IntegrationOwner::class.java).convention(IntegrationOwner.LAZY)
    public val side: Property<IntegrationSide> = objects.property(IntegrationSide::class.java).convention(IntegrationSide.BOTH)
    public val requiredMods: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    public val optionalMods: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    public val integrationDependencies: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    public val dataGen: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    public fun requiredMod(
        id: String,
        versionRef: String,
    ) {
        requiredMods.add("$id|$versionRef")
    }

    public fun optionalMod(
        id: String,
        versionRef: String,
    ) {
        optionalMods.add("$id|$versionRef")
    }

    public fun dependsOn(id: String) {
        integrationDependencies.add(id)
    }
}
