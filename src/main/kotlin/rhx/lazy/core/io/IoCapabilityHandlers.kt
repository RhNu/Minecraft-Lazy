package rhx.lazy.core.io

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler

internal class IoItemHandler(
    private val input: IItemHandler?,
    private val output: IItemHandler?,
    private val sideMode: () -> SideIoMode,
) : IItemHandler {
    private val shared = input != null && input === output

    override fun getSlots(): Int = if (shared) input!!.slots else (input?.slots ?: 0) + (output?.slots ?: 0)

    override fun getStackInSlot(slot: Int): ItemStack {
        validateSlot(slot)
        return when {
            shared -> input!!.getStackInSlot(slot)
            slot < (input?.slots ?: 0) -> input!!.getStackInSlot(slot)
            else -> output!!.getStackInSlot(slot - (input?.slots ?: 0))
        }
    }

    override fun insertItem(
        slot: Int,
        stack: ItemStack,
        simulate: Boolean,
    ): ItemStack {
        validateSlot(slot)
        if (!sideMode().allowsInput || input == null) return stack
        if (!shared && slot >= input.slots) return stack
        return input.insertItem(slot, stack, simulate)
    }

    override fun extractItem(
        slot: Int,
        amount: Int,
        simulate: Boolean,
    ): ItemStack {
        validateSlot(slot)
        if (!sideMode().allowsOutput || output == null) return ItemStack.EMPTY
        if (shared) return output.extractItem(slot, amount, simulate)
        val outputSlot = slot - (input?.slots ?: 0)
        return if (outputSlot in 0 until output.slots) output.extractItem(outputSlot, amount, simulate) else ItemStack.EMPTY
    }

    override fun getSlotLimit(slot: Int): Int {
        validateSlot(slot)
        return when {
            shared -> input!!.getSlotLimit(slot)
            slot < (input?.slots ?: 0) -> input!!.getSlotLimit(slot)
            else -> output!!.getSlotLimit(slot - (input?.slots ?: 0))
        }
    }

    override fun isItemValid(
        slot: Int,
        stack: ItemStack,
    ): Boolean {
        validateSlot(slot)
        return sideMode().allowsInput && input != null && (shared || slot < input.slots) && input.isItemValid(slot, stack)
    }

    private fun validateSlot(slot: Int) {
        if (slot !in 0 until slots) throw IndexOutOfBoundsException("IO item slot $slot is out of range")
    }
}

internal class IoFluidHandler(
    private val input: IFluidHandler?,
    private val output: IFluidHandler?,
    private val sideMode: () -> SideIoMode,
) : IFluidHandler {
    private val shared = input != null && input === output

    override fun getTanks(): Int = if (shared) input!!.tanks else (input?.tanks ?: 0) + (output?.tanks ?: 0)

    override fun getFluidInTank(tank: Int): FluidStack {
        validateTank(tank)
        return when {
            shared -> input!!.getFluidInTank(tank)
            tank < (input?.tanks ?: 0) -> input!!.getFluidInTank(tank)
            else -> output!!.getFluidInTank(tank - (input?.tanks ?: 0))
        }
    }

    override fun getTankCapacity(tank: Int): Int {
        validateTank(tank)
        return when {
            shared -> input!!.getTankCapacity(tank)
            tank < (input?.tanks ?: 0) -> input!!.getTankCapacity(tank)
            else -> output!!.getTankCapacity(tank - (input?.tanks ?: 0))
        }
    }

    override fun isFluidValid(
        tank: Int,
        stack: FluidStack,
    ): Boolean {
        validateTank(tank)
        return sideMode().allowsInput && input != null && (shared || tank < input.tanks) && input.isFluidValid(tank, stack)
    }

    override fun fill(
        resource: FluidStack,
        action: IFluidHandler.FluidAction,
    ): Int = if (sideMode().allowsInput) input?.fill(resource, action) ?: 0 else 0

    override fun drain(
        resource: FluidStack,
        action: IFluidHandler.FluidAction,
    ): FluidStack = if (sideMode().allowsOutput) output?.drain(resource, action) ?: FluidStack.EMPTY else FluidStack.EMPTY

    override fun drain(
        maxDrain: Int,
        action: IFluidHandler.FluidAction,
    ): FluidStack = if (sideMode().allowsOutput) output?.drain(maxDrain, action) ?: FluidStack.EMPTY else FluidStack.EMPTY

    private fun validateTank(tank: Int) {
        if (tank !in 0 until tanks) throw IndexOutOfBoundsException("IO fluid tank $tank is out of range")
    }
}
