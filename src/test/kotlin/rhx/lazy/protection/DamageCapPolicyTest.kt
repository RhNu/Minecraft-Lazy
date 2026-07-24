package rhx.lazy.protection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DamageCapPolicyTest {
    @Test
    fun `missing and disabled settings do not alter damage`() {
        val disabled = DamageCapData(enabled = false, threshold = 5)

        assertFalse(DamageCapPolicy.grantsInvulnerability(null, bypassesProtection = false))
        assertFalse(DamageCapPolicy.grantsInvulnerability(disabled, bypassesProtection = false))
        assertEquals(10.0f, DamageCapPolicy.capIncomingDamage(null, 10.0f, bypassesProtection = false))
        assertEquals(10.0f, DamageCapPolicy.capIncomingDamage(disabled, 10.0f, bypassesProtection = false))
    }

    @Test
    fun `zero threshold grants invulnerability without changing incoming amount`() {
        val immune = DamageCapData(enabled = true, threshold = 0)

        assertTrue(DamageCapPolicy.grantsInvulnerability(immune, bypassesProtection = false))
        assertEquals(10.0f, DamageCapPolicy.capIncomingDamage(immune, 10.0f, bypassesProtection = false))
    }

    @Test
    fun `positive threshold caps only larger incoming damage`() {
        val capped = DamageCapData(enabled = true, threshold = 5)

        assertEquals(4.0f, DamageCapPolicy.capIncomingDamage(capped, 4.0f, bypassesProtection = false))
        assertEquals(5.0f, DamageCapPolicy.capIncomingDamage(capped, 5.0f, bypassesProtection = false))
        assertEquals(5.0f, DamageCapPolicy.capIncomingDamage(capped, 9.0f, bypassesProtection = false))
    }

    @Test
    fun `bypassing damage ignores zero and positive thresholds`() {
        val immune = DamageCapData(enabled = true, threshold = 0)
        val capped = DamageCapData(enabled = true, threshold = 5)

        assertFalse(DamageCapPolicy.grantsInvulnerability(immune, bypassesProtection = true))
        assertEquals(9.0f, DamageCapPolicy.capIncomingDamage(capped, 9.0f, bypassesProtection = true))
    }
}
