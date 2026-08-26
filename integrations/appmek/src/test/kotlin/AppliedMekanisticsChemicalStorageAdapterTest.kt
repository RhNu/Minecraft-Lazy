package rhx.lazy.integration.appmek

import me.ramidzkh.mekae2.ae2.MekanismKey
import mekanism.common.registries.MekanismChemicals
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.integration.mekanism.ChemicalResourceKind
import rhx.lazy.integration.mekanism.ChemicalVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppliedMekanisticsChemicalStorageAdapterTest {
    @Test
    fun `chemical resource converts to the Applied Mekanistics AE key`() {
        val chemical = MekanismChemicals.HYDROGEN.asStack(9_000_000_000L)
        val variant = assertNotNull(ChemicalVariant.of(chemical))

        val payload =
            assertNotNull(
                AppliedMekanisticsChemicalStorageAdapter.convert(
                    ResourceAmount(ChemicalResourceKind, variant, chemical.amount),
                ),
            )

        val key = payload.key as MekanismKey
        assertEquals(MekanismChemicals.HYDROGEN.get(), key.stack.chemical)
        assertEquals(9_000_000_000L, payload.amount)
        assertEquals(1L, variant.template.amount)
    }
}
