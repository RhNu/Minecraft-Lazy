package rhx.lazy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratedIntegrationCatalogTest {
    @Test
    fun `common startup catalog is loadable without optional APIs`() {
        val loader = javaClass.classLoader
        commonSafeClasses.forEach { className ->
            Class.forName(className, false, loader)
        }
    }

    @Test
    fun `manifest is deterministic and common catalog excludes client bridges`() {
        val manifest =
            requireNotNull(javaClass.getResourceAsStream("/META-INF/lazy/integrations.json"))
                .bufferedReader()
                .use { reader -> reader.readText() }
        val ids = Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").findAll(manifest).map { it.groupValues[1] }.toList()

        assertTrue(ids.containsAll(listOf("ae2", "appflux", "curios", "jei")))
        assertTrue(ids.indexOf("ae2") < ids.indexOf("appflux"))

        val commonCatalog =
            requireNotNull(
                javaClass.classLoader.getResourceAsStream(
                    "rhx/lazy/generated/integration/catalog/GeneratedCommonIntegrationCatalog.class",
                ),
            ).readAllBytes().decodeToString()
        assertFalse(commonCatalog.contains("GeneratedClientIntegrationCatalog"))
        assertFalse(commonCatalog.contains("CuriosClient"))
    }

    private companion object {
        val commonSafeClasses =
            listOf(
                "rhx.lazy.Lazy",
                "rhx.lazy.generated.integration.catalog.GeneratedCommonIntegrationCatalog",
                "rhx.lazy.generated.integration.ae2.Ae2IntegrationBridge",
                "rhx.lazy.generated.integration.appflux.AppfluxIntegrationBridge",
                "rhx.lazy.generated.integration.beyonddimensions.BeyonddimensionsIntegrationBridge",
                "rhx.lazy.generated.integration.curios.CuriosIntegrationBridge",
                "rhx.lazy.generated.integration.mekanism.MekanismIntegrationBridge",
                "rhx.lazy.generated.integration.mysticalagriculture.MysticalagricultureIntegrationBridge",
                "rhx.lazy.generated.integration.silentgear.SilentgearIntegrationBridge",
            )
    }
}
