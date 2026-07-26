package rhx.lazy.feature.buffer

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.ManagedBlockEntity
import rhx.lazy.integration.beyonddimensions.BeyondDimensionsIntegration
import rhx.lazy.integration.beyonddimensions.DimensionNetworkId
import rhx.lazy.integration.beyonddimensions.DimensionNetworkResult
import rhx.lazy.integration.beyonddimensions.DimensionNetworkStorage
import kotlin.math.min

internal class BufferBlockEntity(
    pos: BlockPos,
    state: BlockState,
    private val dimensionNetworkStorage: DimensionNetworkStorage = BeyondDimensionsIntegration,
) : ManagedBlockEntity(BufferRegistries.blockEntity.get(), pos, state) {
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

    @field:Persisted
    @field:LazyManaged
    private var networkForwardingEnabled = false

    @field:Persisted
    @field:LazyManaged
    private var dimensionNetworkId = INVALID_NETWORK_ID

    val itemHandler: IItemHandlerModifiable = BufferItemHandler()
    val fluidHandler: IFluidHandler = BufferFluidHandler()

    val totalItemCount: Int
        get() = itemTotal

    val totalFluidAmount: Int
        get() = fluidTotal

    val isNetworkForwardingEnabled: Boolean
        get() = networkForwardingEnabled

    val boundDimensionNetworkId: Int?
        get() = dimensionNetworkId.takeIf { it >= 0 }

    fun hasContents(): Boolean = totalItemCount > 0 || totalFluidAmount > 0

    fun clearContents(): Boolean {
        if (!hasContents()) return false
        clearWithoutNotification()
        contentsChanged(ITEM_TEMPLATES_FIELD, ITEM_COUNTS_FIELD, FLUIDS_FIELD)
        return true
    }

    fun enableNetworkForwarding(networkId: DimensionNetworkId) {
        networkForwardingEnabled = true
        dimensionNetworkId = networkId.value
        networkStateChanged()
        flushContentsToNetwork()
    }

    fun disableNetworkForwarding() {
        if (!networkForwardingEnabled && dimensionNetworkId == INVALID_NETWORK_ID) return
        networkForwardingEnabled = false
        dimensionNetworkId = INVALID_NETWORK_ID
        networkStateChanged()
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
        if (networkForwardingEnabled && dimensionNetworkId < 0) {
            networkForwardingEnabled = false
            dimensionNetworkId = INVALID_NETWORK_ID
        }
    }

    private fun contentsChanged(vararg fields: String) {
        itemTotal = itemCounts.sum()
        fluidTotal = fluids.sumOf(FluidStack::getAmount)
        fields.forEach(::markDirty)
        markDirty(ITEM_TOTAL_FIELD)
        markDirty(FLUID_TOTAL_FIELD)
    }

    private fun networkStateChanged() {
        markDirty(NETWORK_FORWARDING_FIELD)
        markDirty(DIMENSION_NETWORK_ID_FIELD)
    }

    private fun networkIdOrNull(): DimensionNetworkId? =
        if (networkForwardingEnabled && dimensionNetworkId >= 0) {
            DimensionNetworkId(dimensionNetworkId)
        } else {
            null
        }

    private fun flushContentsToNetwork() {
        val networkId = networkIdOrNull() ?: return
        if (level?.isClientSide == true) return

        var itemsChanged = false
        var fluidsChanged = false
        var failed = false

        for (slot in itemTemplates.indices) {
            val template = itemTemplates[slot]
            val stored = itemCounts[slot]
            if (template.isEmpty || stored <= 0) continue

            when (
                val result =
                    dimensionNetworkStorage.insertItem(
                        networkId,
                        template.copyWithCount(stored),
                        simulate = false,
                    )
            ) {
                is DimensionNetworkResult.Success -> {
                    val remainder = result.value.count.coerceIn(0, stored)
                    if (remainder != stored) {
                        itemCounts[slot] = remainder
                        if (remainder == 0) itemTemplates[slot] = ItemStack.EMPTY
                        itemsChanged = true
                    }
                }

                else -> {
                    failed = true
                    break
                }
            }
        }

        if (!failed) {
            for (tank in fluids.indices) {
                val stored = fluids[tank]
                if (stored.isEmpty || stored.amount <= 0) continue

                when (val result = dimensionNetworkStorage.insertFluid(networkId, stored, simulate = false)) {
                    is DimensionNetworkResult.Success -> {
                        val remainder = result.value.amount.coerceIn(0, stored.amount)
                        if (remainder != stored.amount) {
                            fluids[tank] =
                                if (remainder == 0) {
                                    FluidStack.EMPTY
                                } else {
                                    stored.copyWithAmount(remainder)
                                }
                            fluidsChanged = true
                        }
                    }

                    else -> {
                        failed = true
                        break
                    }
                }
            }
        }

        if (itemsChanged || fluidsChanged) {
            val fields = mutableListOf<String>()
            if (itemsChanged) fields += listOf(ITEM_TEMPLATES_FIELD, ITEM_COUNTS_FIELD)
            if (fluidsChanged) fields += FLUIDS_FIELD
            contentsChanged(*fields.toTypedArray())
        }
        if (failed) disableNetworkForwarding()
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
            if (networkIdOrNull() == null && !isItemValid(slot, stack)) return stack

            val forwardedRemainder = forwardItem(stack, simulate)
            if (!isItemLocallyValid(slot, forwardedRemainder)) return forwardedRemainder
            return insertItemLocally(slot, forwardedRemainder, simulate)
        }

        private fun forwardItem(
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            val networkId = networkIdOrNull() ?: return stack
            if (level?.isClientSide == true) return stack

            return when (val result = dimensionNetworkStorage.insertItem(networkId, stack, simulate)) {
                is DimensionNetworkResult.Success -> result.value
                else -> {
                    if (!simulate) disableNetworkForwarding()
                    stack
                }
            }
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
            return networkIdOrNull() != null || isItemLocallyValid(slot, stack)
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
            val forwardedRemainder = forwardFluid(resource, action.simulate())
            val forwarded = resource.amount - forwardedRemainder.amount
            return forwarded + fillLocally(forwardedRemainder, action)
        }

        private fun forwardFluid(
            resource: FluidStack,
            simulate: Boolean,
        ): FluidStack {
            val networkId = networkIdOrNull() ?: return resource
            if (level?.isClientSide == true) return resource

            return when (val result = dimensionNetworkStorage.insertFluid(networkId, resource, simulate)) {
                is DimensionNetworkResult.Success -> result.value
                else -> {
                    if (!simulate) disableNetworkForwarding()
                    resource
                }
            }
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

    companion object {
        const val ITEM_SLOT_COUNT = 8
        const val ITEM_SLOT_CAPACITY = 256
        const val FLUID_TANK_COUNT = 4
        const val FLUID_TANK_CAPACITY = 64_000
        const val TOTAL_ITEM_CAPACITY = ITEM_SLOT_COUNT * ITEM_SLOT_CAPACITY
        const val TOTAL_FLUID_CAPACITY = FLUID_TANK_COUNT * FLUID_TANK_CAPACITY

        internal const val MANAGED_DATA_KEY = "managed"
        internal const val ITEM_TOTAL_FIELD = "itemTotal"
        internal const val FLUID_TOTAL_FIELD = "fluidTotal"
        internal const val NETWORK_FORWARDING_FIELD = "networkForwardingEnabled"
        internal const val DIMENSION_NETWORK_ID_FIELD = "dimensionNetworkId"

        private const val ITEM_TEMPLATES_FIELD = "itemTemplates"
        private const val ITEM_COUNTS_FIELD = "itemCounts"
        private const val FLUIDS_FIELD = "fluids"
        private const val INVALID_NETWORK_ID = -1
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
