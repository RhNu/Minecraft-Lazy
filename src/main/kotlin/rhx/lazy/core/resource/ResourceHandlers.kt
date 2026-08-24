package rhx.lazy.core.resource

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import kotlin.math.min

/** NeoForge-sized view over a long-count item store. */
internal class ResourceItemHandler(
    private val store: ResourceStore<ItemVariant>,
    private val allowInsert: Boolean = true,
    private val allowExtract: Boolean = true,
    private val isValid: (ItemStack) -> Boolean = { true },
) : IItemHandlerModifiable {
    override fun getSlots(): Int = store.slots

    override fun getStackInSlot(slot: Int): ItemStack {
        val variant = store.variant(slot) ?: return ItemStack.EMPTY
        val count =
            min(
                store.amount(slot),
                variant.template.maxStackSize
                    .coerceAtLeast(1)
                    .toLong(),
            ).toInt()
        return variant.template.copyWithCount(count)
    }

    override fun insertItem(
        slot: Int,
        stack: ItemStack,
        simulate: Boolean,
    ): ItemStack {
        val amount = itemAmount(stack) ?: return stack
        if (!isItemValid(slot, stack)) return stack
        val inserted = store.insertIntoSlot(slot, amount, simulate).toInt()
        return if (inserted >= stack.count) ItemStack.EMPTY else stack.copyWithCount(stack.count - inserted)
    }

    override fun extractItem(
        slot: Int,
        amount: Int,
        simulate: Boolean,
    ): ItemStack {
        if (!allowExtract || amount <= 0) return ItemStack.EMPTY
        val variant = store.variant(slot) ?: return ItemStack.EMPTY
        val limit = min(amount, variant.template.maxStackSize.coerceAtLeast(1)).toLong()
        return store.extract(slot, limit, simulate)?.let { variant.template.copyWithCount(it.amount.toInt()) } ?: ItemStack.EMPTY
    }

    override fun getSlotLimit(slot: Int): Int {
        store.amount(slot)
        return store.amountLimit.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    override fun isItemValid(
        slot: Int,
        stack: ItemStack,
    ): Boolean {
        store.amount(slot)
        if (!allowInsert || stack.isEmpty || !isValid(stack)) return false
        val variant = ItemVariant.of(stack) ?: return false
        return store.variant(slot)?.let { ItemResourceKind.matches(it, variant) } != false
    }

    override fun setStackInSlot(
        slot: Int,
        stack: ItemStack,
    ) {
        store.amount(slot)
        store.replace(slot, itemAmount(stack))
    }
}

/** NeoForge-sized view over a long-count fluid store. */
internal class ResourceFluidHandler(
    private val store: ResourceStore<FluidVariant>,
    private val allowInsert: Boolean = true,
    private val allowExtract: Boolean = true,
    private val isValid: (FluidStack) -> Boolean = { true },
) : IFluidHandler {
    override fun getTanks(): Int = store.slots

    override fun getFluidInTank(tank: Int): FluidStack {
        val variant = store.variant(tank) ?: return FluidStack.EMPTY
        return variant.template.copyWithAmount(store.amount(tank).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    override fun getTankCapacity(tank: Int): Int {
        store.amount(tank)
        return store.amountLimit.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    override fun isFluidValid(
        tank: Int,
        stack: FluidStack,
    ): Boolean {
        store.amount(tank)
        if (!allowInsert || stack.isEmpty || !isValid(stack)) return false
        val variant = FluidVariant.of(stack) ?: return false
        return store.variant(tank)?.let { FluidResourceKind.matches(it, variant) } != false
    }

    override fun fill(
        resource: FluidStack,
        action: IFluidHandler.FluidAction,
    ): Int {
        if (!allowInsert || resource.isEmpty || !isValid(resource)) return 0
        val amount = fluidAmount(resource) ?: return 0
        return store.insert(amount, action.simulate()).toInt()
    }

    override fun drain(
        resource: FluidStack,
        action: IFluidHandler.FluidAction,
    ): FluidStack {
        if (!allowExtract || resource.isEmpty) return FluidStack.EMPTY
        val requested = FluidVariant.of(resource) ?: return FluidStack.EMPTY
        var remaining = resource.amount.toLong()
        var drained = 0L
        for (tank in 0 until store.slots) {
            val stored = store.variant(tank) ?: continue
            if (!FluidResourceKind.matches(stored, requested)) continue
            val extracted = min(remaining, store.amount(tank))
            if (extracted <= 0L) continue
            if (action.execute()) store.extract(tank, extracted)
            drained += extracted
            remaining -= extracted
            if (remaining <= 0L) break
        }
        return if (drained <= 0L) FluidStack.EMPTY else requested.template.copyWithAmount(drained.toInt())
    }

    override fun drain(
        maxDrain: Int,
        action: IFluidHandler.FluidAction,
    ): FluidStack {
        if (!allowExtract || maxDrain <= 0) return FluidStack.EMPTY
        for (tank in 0 until store.slots) {
            val stored = store.variant(tank) ?: continue
            return drain(stored.template.copyWithAmount(maxDrain), action)
        }
        return FluidStack.EMPTY
    }
}
