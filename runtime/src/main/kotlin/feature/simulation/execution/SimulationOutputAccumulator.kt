package rhx.lazy.feature.simulation

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.process.PreparedCommit
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount

/** Collects one random result slice, merging exact variants with checked [Long] arithmetic. */
internal class SimulationOutputAccumulator(
    private val level: ServerLevel,
    private val processItem: (ServerLevel, ItemStack) -> List<ItemStack> = { _, stack -> listOf(stack) },
) {
    private val items = mutableListOf<ItemAmount>()
    private val fluids = mutableListOf<FluidAmount>()

    fun add(
        stack: ItemStack,
        amount: Long = stack.count.toLong(),
    ) {
        if (stack.isEmpty || amount <= 0L) return
        val count = stack.count.toLong()
        val copies = amount / count
        val remainder = (amount % count).toInt()
        repeat(Math.toIntExact(copies)) {
            processItem(level, stack).forEach(::addProcessed)
        }
        if (remainder > 0) processItem(level, stack.copyWithCount(remainder)).forEach(::addProcessed)
    }

    fun add(
        stack: FluidStack,
        amount: Long = stack.amount.toLong(),
    ) {
        if (stack.isEmpty || amount <= 0L) return
        val existing = fluids.firstOrNull { FluidStack.isSameFluidSameComponents(it.template, stack) }
        if (existing == null) {
            fluids += FluidAmount(stack.copyWithAmount(1), amount)
        } else {
            existing.amount = Math.addExact(existing.amount, amount)
        }
    }

    fun prepare(workUnits: Int): PreparedCommit =
        PreparedCommit(
            items =
                items.mapNotNull { entry ->
                    ItemVariant.of(entry.template)?.let { ResourceAmount(ItemResourceKind, it, entry.amount) }
                },
            fluids =
                fluids.mapNotNull { entry ->
                    FluidVariant.of(entry.template)?.let { ResourceAmount(FluidResourceKind, it, entry.amount) }
                },
            workUnits = workUnits,
        )

    private data class ItemAmount(
        val template: ItemStack,
        var amount: Long,
    )

    private data class FluidAmount(
        val template: FluidStack,
        var amount: Long,
    )

    private fun addProcessed(stack: ItemStack) {
        if (stack.isEmpty) return
        val existing = items.firstOrNull { ItemStack.isSameItemSameComponents(it.template, stack) }
        if (existing == null) {
            items += ItemAmount(stack.copyWithCount(1), stack.count.toLong())
        } else {
            existing.amount = Math.addExact(existing.amount, stack.count.toLong())
        }
    }
}
