package rhx.lazy.core.config

import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.ModConfigSpec
import rhx.lazy.integration.api.LazyInternalApi

/** Read-only configuration value exposed to feature code. */
@LazyInternalApi
public class LazyConfigValue<T : Any> internal constructor(
    private val getter: () -> T,
    private val defaultGetter: () -> T,
) {
    private var isLoaded: () -> Boolean = { false }

    fun get(): T = if (isLoaded()) getter() else defaultGetter()

    public fun bind(isLoaded: () -> Boolean) {
        this.isLoaded = isLoaded
    }
}

/** Small Kotlin-facing subset of [ModConfigSpec.Builder] used by Lazy. */
@LazyInternalApi
public class LazyConfigBuilder internal constructor(
    private val delegate: ModConfigSpec.Builder,
) {
    private val values = mutableListOf<LazyConfigValue<*>>()

    fun int(
        key: String,
        defaultValue: Int,
        range: IntRange,
        comment: String,
    ): LazyConfigValue<Int> =
        delegate
            .comment(comment)
            .defineInRange(key, defaultValue, range.first, range.last)
            .let(::wrap)

    fun long(
        key: String,
        defaultValue: Long,
        range: LongRange,
        comment: String,
    ): LazyConfigValue<Long> =
        delegate
            .comment(comment)
            .defineInRange(key, defaultValue, range.first, range.last)
            .let(::wrap)

    fun boolean(
        key: String,
        defaultValue: Boolean,
        comment: String,
    ): LazyConfigValue<Boolean> =
        delegate
            .comment(comment)
            .define(key, defaultValue)
            .let(::wrap)

    fun stringList(
        key: String,
        defaultValue: List<String>,
        comment: String,
    ): LazyConfigValue<List<String>> {
        val value =
            delegate
                .comment(comment)
                .defineListAllowEmpty(key, defaultValue, { "" }) { candidate -> candidate is String }
        return LazyConfigValue(
            getter = { value.get().toList() },
            defaultGetter = { value.default.toList() },
        ).also(values::add)
    }

    public fun bind(spec: ModConfigSpec) {
        values.forEach { value -> value.bind(spec::isLoaded) }
    }

    private fun <T : Any> wrap(value: ModConfigSpec.ConfigValue<T>): LazyConfigValue<T> =
        LazyConfigValue(value::get, value::getDefault).also(values::add)
}

/** A built specification and its typed settings object. */
@LazyInternalApi
public class LazyConfigDefinition<T> private constructor(
    val settings: T,
    public val spec: ModConfigSpec,
) {
    fun registerServer(
        container: ModContainer,
        fileName: String,
    ) {
        require(fileName.endsWith(".toml")) { "Server config filename must end with .toml" }
        container.registerConfig(ModConfig.Type.SERVER, spec, fileName)
    }

    companion object {
        fun <T> create(factory: (LazyConfigBuilder) -> T): LazyConfigDefinition<T> {
            val builder = ModConfigSpec.Builder()
            val lazyBuilder = LazyConfigBuilder(builder)
            val settings = factory(lazyBuilder)
            val spec = builder.build()
            lazyBuilder.bind(spec)
            return LazyConfigDefinition(settings, spec)
        }
    }
}
