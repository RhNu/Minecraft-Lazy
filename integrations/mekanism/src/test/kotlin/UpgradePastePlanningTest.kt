package rhx.lazy.integration.mekanism

import kotlin.test.Test
import kotlin.test.assertEquals

class UpgradePastePlanningTest {
    @Test
    fun `plan only fills deficits and never removes extra upgrades`() {
        val plan =
            UpgradePastePlan.create(
                listOf("speed", "energy", "anchor"),
                desired = mapOf("speed" to 8, "energy" to 4, "anchor" to 1),
                current = mapOf("speed" to 3, "energy" to 8, "anchor" to 1),
            )

        assertEquals(mapOf("speed" to 5), plan.required)
    }

    @Test
    fun `outcomes distinguish complete partial and no installation`() {
        val plan = UpgradePastePlan(mapOf("speed" to 5, "energy" to 3))

        assertEquals(UpgradePasteStatus.COMPLETE, plan.status(mapOf("speed" to 5, "energy" to 3)))
        assertEquals(UpgradePasteStatus.PARTIAL, plan.status(mapOf("speed" to 2)))
        assertEquals(UpgradePasteStatus.NONE_INSTALLED, plan.status(emptyMap()))
        assertEquals(mapOf("speed" to 3, "energy" to 3), plan.missing(mapOf("speed" to 2)))
    }

    @Test
    fun `empty deficit means the target is already satisfied`() {
        val plan = UpgradePastePlan.create(listOf("speed"), mapOf("speed" to 4), mapOf("speed" to 8))

        assertEquals(UpgradePasteStatus.ALREADY_SATISFIED, plan.status(emptyMap()))
    }
}
