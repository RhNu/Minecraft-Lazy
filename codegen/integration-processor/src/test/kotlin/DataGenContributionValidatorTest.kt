package rhx.lazy.integration.processor

import kotlin.test.Test
import kotlin.test.assertFailsWith

class DataGenContributionValidatorTest {
    @Test
    fun `rejects contributions whose DSL module disables DataGen`() {
        assertFailsWith<IllegalArgumentException> {
            DataGenContributionValidator.validate(setOf("runtime"), listOf("runtime", "kubejs"))
        }
    }

    @Test
    fun `rejects DataGen modules without a contribution`() {
        assertFailsWith<IllegalArgumentException> {
            DataGenContributionValidator.validate(setOf("runtime", "ae2"), listOf("runtime"))
        }
    }
}
