package rhx.lazy.integration.mekanism

import mekanism.common.registries.MekanismChemicals
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKinds
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MekanismChemicalResourcesTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @BeforeTest
    fun installResourceSupport() {
        if (ResourceKinds[ChemicalResourceKind.id] == null) MekanismChemicalResourceIntegration.install()
    }

    @Test
    fun `chemical identity persists independently from its long amount`() {
        val source = MekanismChemicals.HYDROGEN.asStack(9_000_000_000L)
        val variant = assertNotNull(ChemicalVariant.of(source))
        val original = ResourceAmount(ChemicalResourceKind, variant, source.amount)

        val restored = assertNotNull(ResourceAmount.parse(registries, original.save(registries)))

        assertEquals(ChemicalResourceKind, restored.kind)
        assertEquals(9_000_000_000L, restored.amount)
        assertTrue(original.matches(restored))
        assertEquals(1L, variant.template.amount)
        assertEquals(MekanismChemicals.HYDROGEN.get(), (restored.variant as ChemicalVariant).template.chemical)
    }
}
