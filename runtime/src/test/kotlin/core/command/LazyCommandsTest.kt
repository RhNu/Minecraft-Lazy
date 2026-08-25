package rhx.lazy.core.command

import kotlin.test.Test
import kotlin.test.assertTrue

class LazyCommandsTest {
    @Test
    fun `contribution cannot replace a built-in subcommand`() {
        val failure = runCatching { LazyCommands.contribute("rise", LazySubcommand { }) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
    }

    @Test
    fun `contribution id must be resource-safe`() {
        val failure = runCatching { LazyCommands.contribute("invalid id", LazySubcommand { }) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }
}
