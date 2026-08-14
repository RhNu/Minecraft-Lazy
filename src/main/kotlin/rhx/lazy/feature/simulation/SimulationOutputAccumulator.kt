package rhx.lazy.feature.simulation

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import java.math.BigInteger

internal class SimulationOutputAccumulator {
    private val items = mutableListOf<ItemAmount>()
    private val fluids = mutableListOf<FluidAmount>()

    fun add(
        stack: ItemStack,
        amount: Long = stack.count.toLong(),
    ) {
        if (stack.isEmpty || amount <= 0L) return
        val existing = items.firstOrNull { ItemStack.isSameItemSameComponents(it.template, stack) }
        if (existing == null) {
            items += ItemAmount(stack.copyWithCount(1), BigInteger.valueOf(amount))
        } else {
            existing.amount += BigInteger.valueOf(amount)
        }
    }

    fun add(
        stack: FluidStack,
        amount: Long = stack.amount.toLong(),
    ) {
        if (stack.isEmpty || amount <= 0L) return
        val existing = fluids.firstOrNull { FluidStack.isSameFluidSameComponents(it.template, stack) }
        if (existing == null) {
            fluids += FluidAmount(stack.copyWithAmount(1), BigInteger.valueOf(amount))
        } else {
            existing.amount += BigInteger.valueOf(amount)
        }
    }

    fun flush(router: SimulationOutputRouter) {
        items.forEach { entry -> entry.amount.forEachLongChunk { router.enqueue(entry.template, it) } }
        fluids.forEach { entry -> entry.amount.forEachLongChunk { router.enqueue(entry.template, it) } }
        items.clear()
        fluids.clear()
    }

    private data class ItemAmount(
        val template: ItemStack,
        var amount: BigInteger,
    )

    private data class FluidAmount(
        val template: FluidStack,
        var amount: BigInteger,
    )

    private companion object {
        val LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE)

        fun BigInteger.forEachLongChunk(consumer: (Long) -> Unit) {
            var remaining = this
            while (remaining.signum() > 0) {
                val chunk = remaining.min(LONG_MAX)
                consumer(chunk.toLong())
                remaining -= chunk
            }
        }
    }
}
