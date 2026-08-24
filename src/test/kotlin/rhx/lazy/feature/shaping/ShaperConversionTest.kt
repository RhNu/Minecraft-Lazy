package rhx.lazy.feature.shaping

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShaperConversionTest {
    @Test
    fun `conversion reduces unit ratios to the smallest whole trade`() {
        assertEquals(ShaperTrade(9, 1), shaperTrade(16, 144))
        assertEquals(ShaperTrade(1, 9), shaperTrade(144, 16))
        assertEquals(ShaperTrade(1, 2), shaperTrade(144, 72))
        assertEquals(ShaperTrade(8, 1), shaperTrade(72, 576))
        assertEquals(ShaperTrade(9, 4), shaperTrade(576, 1296))
    }

    @Test
    fun `trade count is bounded by both input and output capacity`() {
        val trade = requireNotNull(shaperTrade(1296, 16))

        assertEquals(12, trade.trades(20, 1000))
        assertEquals(0, trade.trades(1, 80))
        assertEquals(1, trade.trades(1, 81))
    }

    @Test
    fun `invalid unit values do not make a trade`() {
        assertNull(shaperTrade(0, 144))
        assertNull(shaperTrade(144, -1))
    }
}
