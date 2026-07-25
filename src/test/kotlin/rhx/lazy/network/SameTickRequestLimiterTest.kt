package rhx.lazy.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SameTickRequestLimiterTest {
    @Test
    fun `each key can acquire at most once per tick`() {
        val limiter = SameTickRequestLimiter<Any>()
        val firstPlayer = Any()
        val secondPlayer = Any()

        assertTrue(limiter.tryAcquire(firstPlayer, 10))
        assertFalse(limiter.tryAcquire(firstPlayer, 10))
        assertTrue(limiter.tryAcquire(secondPlayer, 10))
        assertTrue(limiter.tryAcquire(firstPlayer, 11))
    }
}
