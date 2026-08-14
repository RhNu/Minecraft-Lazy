package rhx.lazy.feature.simulation

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import kotlin.math.min

internal class SimulationOutputItemHandler(
    private val stacks: MutableList<ItemStack>,
    private val changed: () -> Unit,
) : IItemHandlerModifiable {
    override fun getSlots() = stacks.size

    override fun getStackInSlot(slot: Int) = stacks[slot].copy()

    override fun insertItem(
        slot: Int,
        stack: ItemStack,
        simulate: Boolean,
    ) = stack

    override fun extractItem(
        slot: Int,
        amount: Int,
        simulate: Boolean,
    ): ItemStack {
        val stored = stacks[slot]
        if (stored.isEmpty || amount <= 0) return ItemStack.EMPTY
        val result = stored.copyWithCount(min(amount, stored.count))
        if (!simulate) {
            stacks[slot] = if (result.count == stored.count) ItemStack.EMPTY else stored.copyWithCount(stored.count - result.count)
            changed()
        }
        return result
    }

    override fun getSlotLimit(slot: Int) = 64

    override fun isItemValid(
        slot: Int,
        stack: ItemStack,
    ) = false

    override fun setStackInSlot(
        slot: Int,
        stack: ItemStack,
    ) {
        stacks[slot] = stack.copyWithCount(min(stack.count, 64))
        changed()
    }
}

internal class SimulationOutputFluidHandler(
    private val fluids: MutableList<FluidStack>,
    private val tankCapacity: Int,
    private val changed: () -> Unit,
) : IFluidHandler {
    override fun getTanks() = fluids.size

    override fun getFluidInTank(tank: Int) = fluids[tank].copy()

    override fun getTankCapacity(tank: Int) = tankCapacity

    override fun isFluidValid(
        tank: Int,
        stack: FluidStack,
    ) = false

    override fun fill(
        resource: FluidStack,
        action: IFluidHandler.FluidAction,
    ) = 0

    override fun drain(
        resource: FluidStack,
        action: IFluidHandler.FluidAction,
    ): FluidStack {
        if (resource.isEmpty) return FluidStack.EMPTY
        var remaining = resource.amount
        var drained = 0
        fluids.indices.forEach { tank ->
            val stored = fluids[tank]
            if (remaining <= 0 || stored.isEmpty || !FluidStack.isSameFluidSameComponents(stored, resource)) return@forEach
            val amount = min(remaining, stored.amount)
            if (action.execute()) {
                fluids[tank] = if (amount == stored.amount) FluidStack.EMPTY else stored.copyWithAmount(stored.amount - amount)
            }
            remaining -= amount
            drained += amount
        }
        if (action.execute() && drained > 0) changed()
        return resource.copyWithAmount(drained)
    }

    override fun drain(
        maxDrain: Int,
        action: IFluidHandler.FluidAction,
    ): FluidStack {
        if (maxDrain <= 0) return FluidStack.EMPTY
        val first = fluids.firstOrNull { !it.isEmpty } ?: return FluidStack.EMPTY
        return drain(first.copyWithAmount(maxDrain), action)
    }
}
