package rhx.lazy.integration.ae2

import rhx.lazy.core.datagen.LanguageContributions

internal object Ae2Translations {
    fun register() {
        add("gui.lazy.io.provider.ae2", "AE2 ME Network", "AE2 ME 网络")
    }

    private fun add(
        key: String,
        english: String,
        chinese: String,
    ) {
        LanguageContributions.register(key, english, chinese)
    }
}
