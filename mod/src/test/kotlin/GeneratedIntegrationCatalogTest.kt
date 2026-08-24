package rhx.lazy

import kotlin.test.Test

class GeneratedIntegrationCatalogTest {
    @Test
    fun `common startup catalog is loadable without optional APIs`() {
        val loader = javaClass.classLoader
        listOf(
            "rhx.lazy.Lazy",
            "rhx.lazy.generated.integration.catalog.GeneratedCommonIntegrationCatalog",
        ).forEach { className ->
            Class.forName(className, false, loader)
        }
    }
}
