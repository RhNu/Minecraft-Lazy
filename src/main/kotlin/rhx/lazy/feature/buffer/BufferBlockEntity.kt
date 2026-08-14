package rhx.lazy.feature.buffer

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import net.neoforged.neoforge.items.ItemHandlerHelper
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NeighborCapabilities
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOffer
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.offer
import kotlin.math.min

internal class BufferBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(BufferRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private val itemTemplates = MutableList(ITEM_SLOT_COUNT) { ItemStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private val itemCounts = MutableList(ITEM_SLOT_COUNT) { 0 }

    @field:Persisted
    @field:LazyManaged
    private val fluids = MutableList(FLUID_TANK_COUNT) { FluidStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private var itemTotal = 0

    @field:Persisted
    @field:LazyManaged
    private var fluidTotal = 0

    val itemHandler: IItemHandlerModifiable = BufferItemHandler()
    val fluidHandler: IFluidHandler = BufferFluidHandler()

    private val neighborItems = NeighborCapabilities.items(blockPos) { !isRemoved }
    private val neighborFluids = NeighborCapabilities.fluids(blockPos) { !isRemoved }

    init {
        installIoAdapter(BufferIoAdapter())
    }

    val totalItemCount: Int
        get() = itemTotal

    val totalFluidAmount: Int
        get() = fluidTotal

    fun hasContents(): Boolean = totalItemCount > 0 || totalFluidAmount > 0

    fun clearContents(): Boolean {
        if (!hasContents()) return false
        clearWithoutNotification()
        contentsChanged(ITEM_TEMPLATES_FIELD, ITEM_COUNTS_FIELD, FLUIDS_FIELD)
        return true
    }

    fun getItemTemplate(slot: Int): ItemStack {
        validateItemSlot(slot)
        val template = itemTemplates[slot]
        return if (template.isEmpty) ItemStack.EMPTY else template.copy()
    }

    fun getItemCount(slot: Int): Int {
        validateItemSlot(slot)
        return itemCounts[slot]
    }

    fun getFluid(slot: Int): FluidStack {
        validateFluidTank(slot)
        val fluid = fluids[slot]
        return if (fluid.isEmpty) FluidStack.EMPTY else fluid.copy()
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        normalizeContents()
    }

    fun onServerTick() {
        ioController.tick()
    }

    override fun setRemoved() {
        neighborItems.invalidate()
        neighborFluids.invalidate()
        super.setRemoved()
    }

    private fun contentsChanged(vararg fields: String) {
        itemTotal = itemCounts.sum()
        fluidTotal = fluids.sumOf(FluidStack::getAmount)
        fields.forEach(::markDirty)
        markDirty(ITEM_TOTAL_FIELD)
        markDirty(FLUID_TOTAL_FIELD)
    }

    private fun clearWithoutNotification() {
        repeat(ITEM_SLOT_COUNT) { slot ->
            itemTemplates[slot] = ItemStack.EMPTY
            itemCounts[slot] = 0
        }
        repeat(FLUID_TANK_COUNT) { tank -> fluids[tank] = FluidStack.EMPTY }
    }

    private fun normalizeContents() {
        itemTemplates.resize(ITEM_SLOT_COUNT) { ItemStack.EMPTY }
        itemCounts.resize(ITEM_SLOT_COUNT) { 0 }
        fluids.resize(FLUID_TANK_COUNT) { FluidStack.EMPTY }

        repeat(ITEM_SLOT_COUNT) { slot ->
            val template = itemTemplates[slot]
            val count = itemCounts[slot].coerceIn(0, ITEM_SLOT_CAPACITY)
            if (template.isEmpty || count == 0) {
                itemTemplates[slot] = ItemStack.EMPTY
                itemCounts[slot] = 0
            } else {
                itemTemplates[slot] = template.copyWithCount(1)
                itemCounts[slot] = count
            }
        }
        repeat(FLUID_TANK_COUNT) { tank ->
            val fluid = fluids[tank]
            fluids[tank] =
                if (fluid.isEmpty || fluid.amount <= 0) {
                    FluidStack.EMPTY
                } else {
                    fluid.copyWithAmount(fluid.amount.coerceAtMost(FLUID_TANK_CAPACITY))
                }
        }
        itemTotal = itemCounts.sum()
        fluidTotal = fluids.sumOf(FluidStack::getAmount)
    }

    private fun validateItemSlot(slot: Int) {
        if (slot !in itemTemplates.indices) {
            throw IndexOutOfBoundsException("Slot $slot is out of range for buffer")
        }
    }

    private fun validateFluidTank(tank: Int) {
        if (tank !in fluids.indices) {
            throw IndexOutOfBoundsException("Tank $tank is out of range for buffer")
        }
    }

    private inner class BufferItemHandler : IItemHandlerModifiable {
        override fun getSlots(): Int = ITEM_SLOT_COUNT

        override fun getStackInSlot(slot: Int): ItemStack {
            validateItemSlot(slot)
            val template = itemTemplates[slot]
            val count = itemCounts[slot]
            return if (template.isEmpty || count == 0) ItemStack.EMPTY else template.copyWithCount(count)
        }

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            validateItemSlot(slot)
            if (stack.isEmpty) return stack
            if (!isItemValid(slot, stack)) return stack
            return insertItemLocally(slot, stack, simulate)
        }

        private fun insertItemLocally(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            if (stack.isEmpty) return ItemStack.EMPTY
            val inserted = min(stack.count, ITEM_SLOT_CAPACITY - itemCounts[slot])
            if (inserted <= 0) return stack
            if (!simulate) {
                if (itemTemplates[slot].isEmpty) {
                    itemTemplates[slot] = stack.copyWithCount(1)
                }
                itemCounts[slot] += inserted
                contentsChanged(ITEM_TEMPLATES_FIELD, ITEM_COUNTS_FIELD)
            }
            return if (inserted == stack.count) ItemStack.EMPTY else stack.copyWithCount(stack.count - inserted)
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            validateItemSlot(slot)
            val template = itemTemplates[slot]
            val stored = itemCounts[slot]
            if (amount <= 0 || template.isEmpty || stored == 0) return ItemStack.EMPTY

            val extracted = min(amount, min(stored, template.maxStackSize.coerceAtLeast(1)))
            val result = template.copyWithCount(extracted)
            if (!simulate) {
                itemCounts[slot] -= extracted
                if (itemCounts[slot] == 0) itemTemplates[slot] = ItemStack.EMPTY
                contentsChanged(ITEM_TEMPLATES_FIELD, ITEM_COUNTS_FIELD)
            }
            return result
        }

        override fun getSlotLimit(slot: Int): Int {
            validateItemSlot(slot)
            return ITEM_SLOT_CAPACITY
        }

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean {
            validateItemSlot(slot)
            return isItemLocallyValid(slot, stack)
        }

        private fun isItemLocallyValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean {
            val template = itemTemplates[slot]
            return !stack.isEmpty &&
                (template.isEmpty || ItemStack.isSameItemSameComponents(template, stack))
        }

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) {
            validateItemSlot(slot)
            if (stack.isEmpty) {
                itemTemplates[slot] = ItemStack.EMPTY
                itemCounts[slot] = 0
            } else {
                itemTemplates[slot] = stack.copyWithCount(1)
                itemCounts[slot] = stack.count.coerceIn(1, ITEM_SLOT_CAPACITY)
            }
            contentsChanged(ITEM_TEMPLATES_FIELD, ITEM_COUNTS_FIELD)
        }
    }

    private inner class BufferFluidHandler : IFluidHandler {
        override fun getTanks(): Int = FLUID_TANK_COUNT

        override fun getFluidInTank(tank: Int): FluidStack {
            validateFluidTank(tank)
            val fluid = fluids[tank]
            return if (fluid.isEmpty) FluidStack.EMPTY else fluid.copy()
        }

        override fun getTankCapacity(tank: Int): Int {
            validateFluidTank(tank)
            return FLUID_TANK_CAPACITY
        }

        override fun isFluidValid(
            tank: Int,
            stack: FluidStack,
        ): Boolean {
            validateFluidTank(tank)
            val stored = fluids[tank]
            return !stack.isEmpty && (stored.isEmpty || FluidStack.isSameFluidSameComponents(stored, stack))
        }

        override fun fill(
            resource: FluidStack,
            action: IFluidHandler.FluidAction,
        ): Int {
            if (resource.isEmpty || resource.amount <= 0) return 0
            return fillLocally(resource, action)
        }

        private fun fillLocally(
            resource: FluidStack,
            action: IFluidHandler.FluidAction,
        ): Int {
            if (resource.isEmpty || resource.amount <= 0) return 0
            var remaining = resource.amount
            val matching =
                fluids.indices.filter { tank ->
                    val stored = fluids[tank]
                    !stored.isEmpty && FluidStack.isSameFluidSameComponents(stored, resource)
                }
            val empty = fluids.indices.filter { tank -> fluids[tank].isEmpty }

            for (tank in matching + empty) {
                if (remaining == 0) break
                val accepted = min(remaining, FLUID_TANK_CAPACITY - fluids[tank].amount)
                if (accepted <= 0) continue
                if (action.execute()) {
                    val stored = fluids[tank]
                    fluids[tank] =
                        if (stored.isEmpty) {
                            resource.copyWithAmount(accepted)
                        } else {
                            stored.copyWithAmount(stored.amount + accepted)
                        }
                }
                remaining -= accepted
            }

            val filled = resource.amount - remaining
            if (action.execute() && filled > 0) contentsChanged(FLUIDS_FIELD)
            return filled
        }

        override fun drain(
            resource: FluidStack,
            action: IFluidHandler.FluidAction,
        ): FluidStack {
            if (resource.isEmpty || resource.amount <= 0) return FluidStack.EMPTY
            var remaining = resource.amount
            var drained = 0
            for (tank in fluids.indices) {
                val stored = fluids[tank]
                if (stored.isEmpty || !FluidStack.isSameFluidSameComponents(stored, resource)) continue
                val amount = min(remaining, stored.amount)
                if (action.execute()) {
                    val left = stored.amount - amount
                    fluids[tank] = if (left == 0) FluidStack.EMPTY else stored.copyWithAmount(left)
                }
                drained += amount
                remaining -= amount
                if (remaining == 0) break
            }

            if (action.execute() && drained > 0) contentsChanged(FLUIDS_FIELD)
            return if (drained == 0) FluidStack.EMPTY else resource.copyWithAmount(drained)
        }

        override fun drain(
            maxDrain: Int,
            action: IFluidHandler.FluidAction,
        ): FluidStack {
            if (maxDrain <= 0) return FluidStack.EMPTY
            val first = fluids.firstOrNull { fluid -> !fluid.isEmpty } ?: return FluidStack.EMPTY
            return drain(first.copyWithAmount(maxDrain), action)
        }
    }

    private inner class BufferIoAdapter : IoAdapter {
        override val capabilities =
            setOf(NetworkInsertCapabilities.ITEM, NetworkInsertCapabilities.FLUID)

        override fun pushToFaces(directions: Set<Direction>): IoPushResult {
            val serverLevel = level as? ServerLevel ?: return IoPushResult.Retry
            var itemsChanged = false
            var fluidsChanged = false
            directions.forEach { direction ->
                val itemTarget = neighborItems[serverLevel, direction] ?: return@forEach
                itemTemplates.indices.forEach { slot ->
                    val template = itemTemplates[slot]
                    val stored = itemCounts[slot]
                    if (template.isEmpty || stored <= 0) return@forEach
                    val remainder = ItemHandlerHelper.insertItemStacked(itemTarget, template.copyWithCount(stored), false)
                    val remaining = remainder.count.coerceIn(0, stored)
                    if (remaining != stored) {
                        takeItems(slot, remaining)
                        itemsChanged = true
                    }
                }
            }
            directions.forEach { direction ->
                val fluidTarget = neighborFluids[serverLevel, direction] ?: return@forEach
                fluids.indices.forEach { tank ->
                    val stored = fluids[tank]
                    if (stored.isEmpty || stored.amount <= 0) return@forEach
                    val accepted = fluidTarget.fill(stored.copy(), IFluidHandler.FluidAction.EXECUTE)
                    if (accepted > 0) {
                        takeFluid(tank, stored.amount - accepted)
                        fluidsChanged = true
                    }
                }
            }
            notifyPushed(itemsChanged, fluidsChanged)
            return IoPushResult.Success
        }

        override fun pushToNetwork(target: NetworkTargetRef): IoPushResult {
            var itemsChanged = false
            var fluidsChanged = false

            fun finish(result: IoPushResult): IoPushResult {
                notifyPushed(itemsChanged, fluidsChanged)
                return result
            }

            itemTemplates.indices.forEach { slot ->
                val template = itemTemplates[slot]
                val stored = itemCounts[slot]
                if (template.isEmpty || stored <= 0) return@forEach
                when (val offer = target.offer(NetworkPayload.Items(template, stored.toLong()), stored.toLong())) {
                    is NetworkOffer.Accepted -> {
                        if (offer.accepted > 0L) {
                            takeItems(slot, stored - offer.accepted.toInt())
                            itemsChanged = true
                        }
                    }
                    is NetworkOffer.Rejected -> return finish(offer.push)
                }
            }
            fluids.indices.forEach { tank ->
                val stored = fluids[tank]
                if (stored.isEmpty || stored.amount <= 0) return@forEach
                when (val offer = target.offer(NetworkPayload.Fluid(stored), stored.amount.toLong())) {
                    is NetworkOffer.Accepted -> {
                        if (offer.accepted > 0L) {
                            takeFluid(tank, stored.amount - offer.accepted.toInt())
                            fluidsChanged = true
                        }
                    }
                    is NetworkOffer.Rejected -> return finish(offer.push)
                }
            }
            return finish(IoPushResult.Success)
        }
    }

    private fun takeItems(
        slot: Int,
        remaining: Int,
    ) {
        itemCounts[slot] = remaining
        if (remaining == 0) itemTemplates[slot] = ItemStack.EMPTY
    }

    private fun takeFluid(
        tank: Int,
        remaining: Int,
    ) {
        val stored = fluids[tank]
        fluids[tank] = if (remaining <= 0) FluidStack.EMPTY else stored.copyWithAmount(remaining)
    }

    private fun notifyPushed(
        itemsChanged: Boolean,
        fluidsChanged: Boolean,
    ) {
        if (itemsChanged) contentsChanged(ITEM_TEMPLATES_FIELD, ITEM_COUNTS_FIELD)
        if (fluidsChanged) contentsChanged(FLUIDS_FIELD)
    }

    companion object {
        const val ITEM_SLOT_COUNT = 8
        const val ITEM_SLOT_CAPACITY = 256
        const val FLUID_TANK_COUNT = 4
        const val FLUID_TANK_CAPACITY = 64_000
        const val TOTAL_ITEM_CAPACITY = ITEM_SLOT_COUNT * ITEM_SLOT_CAPACITY
        const val TOTAL_FLUID_CAPACITY = FLUID_TANK_COUNT * FLUID_TANK_CAPACITY

        internal const val ITEM_TOTAL_FIELD = "itemTotal"
        internal const val FLUID_TOTAL_FIELD = "fluidTotal"
        private const val ITEM_TEMPLATES_FIELD = "itemTemplates"
        private const val ITEM_COUNTS_FIELD = "itemCounts"
        private const val FLUIDS_FIELD = "fluids"
    }
}

private fun <T> MutableList<T>.resize(
    size: Int,
    defaultValue: () -> T,
) {
    while (this.size > size) {
        removeAt(lastIndex)
    }
    while (this.size < size) {
        add(defaultValue())
    }
}
