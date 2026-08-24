package rhx.lazy.integration.mysticalagriculture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EssenceLedgerTest {
    @Test
    fun `all tiers convert through fixed inferium prices`() {
        EssenceTier.entries.forEach { input ->
            EssenceTier.entries.forEach { target ->
                val requested = target.inferiumValue
                val insertion = EssenceLedger(target).insert(input, requested, Long.MAX_VALUE)
                val totalValue = requested.toLong() * input.inferiumValue

                assertEquals(requested, insertion.accepted)
                assertEquals(totalValue / target.inferiumValue, insertion.ledger.outputCount)
                assertEquals((totalValue % target.inferiumValue).toInt(), insertion.ledger.remainderUnits)
            }
        }
    }

    @Test
    fun `lower essence accumulates as remainder until a target is complete`() {
        val first = EssenceLedger(EssenceTier.PRUDENTIUM).insert(EssenceTier.INFERIUM, 3, 10).ledger
        assertEquals(0L, first.outputCount)
        assertEquals(3, first.remainderUnits)

        val second = first.insert(EssenceTier.INFERIUM, 1, 10).ledger
        assertEquals(1L, second.outputCount)
        assertEquals(0, second.remainderUnits)
    }

    @Test
    fun `calculating insertion does not mutate the source ledger`() {
        val source = EssenceLedger(EssenceTier.SUPREMIUM, outputCount = 2, remainderUnits = 3)
        val insertion = source.insert(EssenceTier.INFERIUM, 253, capacity = 4)

        assertEquals(EssenceLedger(EssenceTier.SUPREMIUM, 3, 0), insertion.ledger)
        assertEquals(EssenceLedger(EssenceTier.SUPREMIUM, 2, 3), source)
    }

    @Test
    fun `capacity accepts only an exactly fitting part of a stack`() {
        val ledger = EssenceLedger(EssenceTier.PRUDENTIUM, outputCount = 9, remainderUnits = 1)
        val insertion = ledger.insert(EssenceTier.INFERIUM, 8, capacity = 10)

        assertEquals(3, insertion.accepted)
        assertEquals(10L, insertion.ledger.outputCount)
        assertEquals(0, insertion.ledger.remainderUnits)
    }

    @Test
    fun `long maximum capacity does not overflow`() {
        val ledger = EssenceLedger(EssenceTier.INSANIUM, Long.MAX_VALUE - 1, 0)
        val insertion = ledger.insert(EssenceTier.INSANIUM, Int.MAX_VALUE, Long.MAX_VALUE)

        assertEquals(1, insertion.accepted)
        assertEquals(Long.MAX_VALUE, insertion.ledger.outputCount)
        assertEquals(0, insertion.ledger.remainderUnits)
    }

    @Test
    fun `extraction obeys the supplied stack limit`() {
        val extraction =
            EssenceLedger(EssenceTier.SUPREMIUM, outputCount = 1_000)
                .extract(Int.MAX_VALUE, limit = 64)

        assertEquals(64, extraction.extracted)
        assertEquals(936L, extraction.ledger.outputCount)
    }

    @Test
    fun `normalization truncates count and remainder at a reduced capacity`() {
        val normalized = EssenceLedger(EssenceTier.TERTIUM, 100, 15).normalize(20)

        assertEquals(20L, normalized.outputCount)
        assertEquals(0, normalized.remainderUnits)
    }

    @Test
    fun `missing insanium downgrades to supremium and discards excess`() {
        val downgraded =
            EssenceLedger(EssenceTier.INSANIUM, 10, 768)
                .downgradeMissingInsanium(capacity = 30)

        assertEquals(EssenceTier.SUPREMIUM, downgraded.target)
        assertEquals(30L, downgraded.outputCount)
        assertEquals(0, downgraded.remainderUnits)
    }

    @Test
    fun `missing insanium saturates multiplication overflow`() {
        val downgraded =
            EssenceLedger(EssenceTier.INSANIUM, Long.MAX_VALUE, 0)
                .downgradeMissingInsanium(capacity = Long.MAX_VALUE)

        assertEquals(EssenceTier.SUPREMIUM, downgraded.target)
        assertEquals(Long.MAX_VALUE, downgraded.outputCount)
        assertEquals(0, downgraded.remainderUnits)
    }

    @Test
    fun `target can change only while empty`() {
        val empty = EssenceLedger(EssenceTier.INFERIUM)
        assertEquals(EssenceTier.SUPREMIUM, empty.withTarget(EssenceTier.SUPREMIUM)?.target)

        val occupied = EssenceLedger(EssenceTier.INFERIUM, outputCount = 1)
        assertNull(occupied.withTarget(EssenceTier.SUPREMIUM))
        assertEquals(occupied, occupied.withTarget(EssenceTier.INFERIUM))
    }

    @Test
    fun `clear removes complete output and remainder`() {
        val cleared = EssenceLedger(EssenceTier.SUPREMIUM, 5, 128).clear()

        assertFalse(cleared.hasContents)
        assertEquals(EssenceTier.SUPREMIUM, cleared.target)
        assertTrue(EssenceLedger(EssenceTier.SUPREMIUM, remainderUnits = 1).hasContents)
    }
}
