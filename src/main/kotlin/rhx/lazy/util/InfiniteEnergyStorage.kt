package rhx.lazy.util

import net.neoforged.neoforge.energy.IEnergyStorage

internal const val ENERGY_TRANSFER_LIMIT = 500_000_000

internal class InfiniteEnergyStorage(
    private val transferLimit: Int = ENERGY_TRANSFER_LIMIT,
) : IEnergyStorage {
    init {
        require(transferLimit >= 0) { "Transfer limit must not be negative" }
    }

    override fun receiveEnergy(
        maxReceive: Int,
        simulate: Boolean,
    ): Int = 0

    override fun extractEnergy(
        maxExtract: Int,
        simulate: Boolean,
    ): Int = maxExtract.coerceIn(0, transferLimit)

    override fun getEnergyStored(): Int = transferLimit

    override fun getMaxEnergyStored(): Int = transferLimit

    override fun canExtract(): Boolean = true

    override fun canReceive(): Boolean = false
}
