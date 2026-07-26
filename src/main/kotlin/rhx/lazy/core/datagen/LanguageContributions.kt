package rhx.lazy.core.datagen

internal object LanguageContributions {
    private val translations = linkedMapOf<String, Translation>()

    fun register(
        key: String,
        english: String,
        chinese: String,
    ) {
        val translation = Translation(english, chinese)
        val existing = translations.putIfAbsent(key, translation)
        check(existing == null || existing == translation) {
            "Conflicting language contribution for $key"
        }
    }

    fun english(): Map<String, String> = translations.mapValues { it.value.english }

    fun chinese(): Map<String, String> = translations.mapValues { it.value.chinese }

    private data class Translation(
        val english: String,
        val chinese: String,
    )
}
