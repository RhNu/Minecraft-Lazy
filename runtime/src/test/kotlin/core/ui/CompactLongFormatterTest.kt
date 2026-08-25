package rhx.lazy.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CompactLongFormatterTest {
    @Test
    fun `values below one thousand remain exact`() {
        assertEquals("-999", CompactLongFormatter.format(-999))
        assertEquals("0", CompactLongFormatter.format(0))
        assertEquals("99", CompactLongFormatter.format(99))
        assertEquals("999", CompactLongFormatter.format(999))
    }

    @Test
    fun `large values use compact metric suffixes`() {
        assertEquals("1K", CompactLongFormatter.format(1_000))
        assertEquals("1.5K", CompactLongFormatter.format(1_500))
        assertEquals("9.9K", CompactLongFormatter.format(9_999))
        assertEquals("10K", CompactLongFormatter.format(10_000))
        assertEquals("12K", CompactLongFormatter.format(12_999))
        assertEquals("999K", CompactLongFormatter.format(999_999))
        assertEquals("1M", CompactLongFormatter.format(1_000_000))
        assertEquals("2.4G", CompactLongFormatter.format(2_400_000_000))
    }

    @Test
    fun `negative values use the same suffix rules`() {
        assertEquals("-1K", CompactLongFormatter.format(-1_000))
        assertEquals("-1.5K", CompactLongFormatter.format(-1_500))
        assertEquals("-12M", CompactLongFormatter.format(-12_999_999))
    }

    @Test
    fun `long boundaries do not overflow`() {
        assertEquals("9.2E", CompactLongFormatter.format(Long.MAX_VALUE))
        assertEquals("-9.2E", CompactLongFormatter.format(Long.MIN_VALUE))
    }
}
