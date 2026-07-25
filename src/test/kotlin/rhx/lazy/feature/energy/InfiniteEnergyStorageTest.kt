package rhx.lazy.feature.energy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfiniteEnergyStorageTest {
    private val storage = InfiniteEnergyStorage()

    @Test
    fun `rejects all received energy`() {
        assertEquals(0, storage.receiveEnergy(1, false))
        assertEquals(0, storage.receiveEnergy(Int.MAX_VALUE, true))
        assertEquals(0, storage.receiveEnergy(-1, false))
    }

    @Test
    fun `extracts up to the transfer limit`() {
        assertEquals(1, storage.extractEnergy(1, false))
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.extractEnergy(ENERGY_TRANSFER_LIMIT, false))
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.extractEnergy(Int.MAX_VALUE, false))
        assertEquals(0, storage.extractEnergy(-1, false))
    }

    @Test
    fun `simulation and repeated extraction do not deplete storage`() {
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.extractEnergy(Int.MAX_VALUE, true))
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.extractEnergy(Int.MAX_VALUE, false))
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.extractEnergy(Int.MAX_VALUE, false))
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.energyStored)
    }

    @Test
    fun `reports fixed capacity and extraction flags`() {
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.energyStored)
        assertEquals(ENERGY_TRANSFER_LIMIT, storage.maxEnergyStored)
        assertTrue(storage.canExtract())
        assertFalse(storage.canReceive())
    }
}
