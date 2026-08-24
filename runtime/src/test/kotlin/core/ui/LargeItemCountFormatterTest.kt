package rhx.lazy.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class LargeItemCountFormatterTest {
    @Test
    fun `small quantities remain exact`() {
        assertEquals("0", CompactItemCountFormatter.format(0))
        assertEquals("99", CompactItemCountFormatter.format(99))
        assertEquals("999", CompactItemCountFormatter.format(999))
    }

    @Test
    fun `large quantities use compact metric suffixes`() {
        assertEquals("1K", CompactItemCountFormatter.format(1_000))
        assertEquals("1.5K", CompactItemCountFormatter.format(1_500))
        assertEquals("12K", CompactItemCountFormatter.format(12_999))
        assertEquals("999K", CompactItemCountFormatter.format(999_999))
        assertEquals("1M", CompactItemCountFormatter.format(1_000_000))
        assertEquals("2.4G", CompactItemCountFormatter.format(2_400_000_000))
        assertEquals("9.2E", CompactItemCountFormatter.format(Long.MAX_VALUE))
    }

    @Test
    fun `negative quantities are normalized for defensive rendering`() {
        assertEquals("0", CompactItemCountFormatter.format(-1))
    }
}
