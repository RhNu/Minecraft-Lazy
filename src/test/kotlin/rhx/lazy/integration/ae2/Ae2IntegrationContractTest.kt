package rhx.lazy.integration.ae2

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Ae2IntegrationContractTest {
    @Test
    fun `generated link card resources are packaged`() {
        val paths =
            listOf(
                "/assets/lazy/models/item/me_output_link_card.json",
                "/assets/lazy/textures/item/me_output_link_card.png",
                "/data/lazy/recipe/me_output_link_card.json",
            )
        paths.forEach { path -> assertNotNull(javaClass.getResource(path), "Missing generated resource $path") }

        val recipe =
            javaClass
                .getResourceAsStream("/data/lazy/recipe/me_output_link_card.json")
                .use { stream -> JsonParser.parseReader(requireNotNull(stream).reader()).asJsonObject }
        assertEquals("ae2", recipe.getAsJsonArray("neoforge:conditions")[0].asJsonObject["modid"].asString)
    }

    @Test
    fun `applied flux energy uses FluxKey and never injects AE power`() {
        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))
        val aeProvider =
            Files.readString(
                projectRoot.resolve("src/main/kotlin/rhx/lazy/integration/ae2/Ae2NetworkOutputProvider.kt"),
            )
        val fluxAdapter =
            Files.readString(
                projectRoot.resolve("src/main/kotlin/rhx/lazy/integration/appflux/AppliedFluxIntegrationModule.kt"),
            )

        assertTrue(aeProvider.contains("storageService.inventory.insert"))
        assertTrue(aeProvider.contains("IActionSource.ofMachine(accessPoint)"))
        assertFalse(aeProvider.contains("injectPower"))
        assertFalse(fluxAdapter.contains("injectPower"))
        assertTrue(fluxAdapter.contains("FluxKey.of(EnergyType.FE)"))
    }

    @Test
    fun `link card texture has an editable SVG source`() {
        val projectRoot = Path.of(requireNotNull(System.getProperty("lazy.projectDir")))

        assertTrue(Files.isRegularFile(projectRoot.resolve("art/item/me_output_link_card.svg")))
    }
}
