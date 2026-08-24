package rhx.lazy.integration.ae2

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Ae2IntegrationContractTest {
    @Test
    fun `configuration card resources are packaged independently of AE2`() {
        val paths =
            listOf(
                "/assets/lazy/models/item/configuration_card.json",
                "/assets/lazy/textures/item/icon/configuration_card.png",
                "/data/lazy/recipe/configuration_card.json",
            )
        paths.forEach { path -> assertNotNull(javaClass.getResource(path), "Missing generated resource $path") }
    }

    @Test
    fun `applied flux energy uses FluxKey and never injects AE power`() {
        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))
        val aeProvider =
            Files.readString(
                projectRoot.resolve("integrations/ae2/src/main/kotlin/Ae2NetworkOutputProvider.kt"),
            )
        val fluxAdapter =
            Files.readString(
                projectRoot.resolve(
                    "integrations/appflux/src/main/kotlin/AppliedFluxIntegrationModule.kt",
                ),
            )

        assertTrue(aeProvider.contains("storageService.inventory.insert"))
        assertTrue(aeProvider.contains("IActionSource.ofMachine(accessPoint)"))
        assertFalse(aeProvider.contains("injectPower"))
        assertFalse(fluxAdapter.contains("injectPower"))
        assertTrue(fluxAdapter.contains("FluxKey.of(EnergyType.FE)"))
    }

    @Test
    fun `configuration card texture has an editable SVG source`() {
        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))

        assertTrue(Files.isRegularFile(projectRoot.resolve("art/item/icon/configuration_card.svg")))
    }
}
