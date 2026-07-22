package rhx.compose.block.entity

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.compose.registry.ModBlockEntities
import kotlin.math.min

internal class BufferBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(ModBlockEntities.buffer.get(), pos, state) {
    private val itemTemplates = MutableList(ITEM_SLOT_COUNT) { ItemStack.EMPTY }
    private val itemCounts = IntArray(ITEM_SLOT_COUNT)
    private val fluids = MutableList(FLUID_TANK_COUNT) { FluidStack.EMPTY }

    val itemHandler: IItemHandlerModifiable = BufferItemHandler()
    val fluidHandler: IFluidHandler = BufferFluidHandler()

    var contentVersion: Long = 0
        private set

    val totalItemCount: Int
        get() = itemCounts.sum()

    val totalFluidAmount: Int
        get() = fluids.sumOf(FluidStack::getAmount)

    fun hasContents(): Boolean = totalItemCount > 0 || totalFluidAmount > 0

    fun clearContents(): Boolean {
        if (!hasContents()) return false
        clearWithoutNotification()
        contentsChanged()
        return true
    }

    fun snapshot(): BufferSnapshot =
        BufferSnapshot(
            itemTemplates.indices.map { slot ->
                val count = itemCounts[slot]
                val template = itemTemplates[slot]
                BufferItemSnapshot(
                    if (template.isEmpty || count == 0) ItemStack.EMPTY else template.copyWithCount(1),
                    count,
                )
            },
            fluids.map { fluid -> if (fluid.isEmpty) FluidStack.EMPTY else fluid.copy() },
        )

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)

        val itemList = ListTag()
        itemTemplates.indices.forEach { slot ->
            val template = itemTemplates[slot]
            val count = itemCounts[slot]
            if (!template.isEmpty && count > 0) {
                itemList.add(
                    CompoundTag().apply {
                        putInt(SLOT_KEY, slot)
                        putInt(COUNT_KEY, count)
                        put(ITEM_KEY, template.save(registries, CompoundTag()))
                    },
                )
            }
        }
        tag.put(ITEMS_KEY, itemList)

        val fluidList = ListTag()
        fluids.indices.forEach { tank ->
            val fluid = fluids[tank]
            if (!fluid.isEmpty) {
                fluidList.add(
                    CompoundTag().apply {
                        putInt(TANK_KEY, tank)
                        put(FLUID_KEY, fluid.save(registries))
                    },
                )
            }
        }
        tag.put(FLUIDS_KEY, fluidList)
        tag.putInt(ITEM_TOTAL_KEY, totalItemCount)
        tag.putInt(FLUID_TOTAL_KEY, totalFluidAmount)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        clearWithoutNotification()

        val itemList = tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND.toInt())
        itemList.forEach { rawEntry ->
            val entry = rawEntry as CompoundTag
            val slot = entry.getInt(SLOT_KEY)
            if (slot !in itemTemplates.indices) return@forEach
            val template = ItemStack.parse(registries, entry.getCompound(ITEM_KEY)).orElse(ItemStack.EMPTY)
            val count = entry.getInt(COUNT_KEY).coerceIn(0, ITEM_SLOT_CAPACITY)
            if (!template.isEmpty && count > 0) {
                itemTemplates[slot] = template.copyWithCount(1)
                itemCounts[slot] = count
            }
        }

        val fluidList = tag.getList(FLUIDS_KEY, Tag.TAG_COMPOUND.toInt())
        fluidList.forEach { rawEntry ->
            val entry = rawEntry as CompoundTag
            val tank = entry.getInt(TANK_KEY)
            if (tank !in fluids.indices) return@forEach
            val fluid = FluidStack.parseOptional(registries, entry.getCompound(FLUID_KEY))
            if (!fluid.isEmpty) {
                fluids[tank] = fluid.copyWithAmount(fluid.amount.coerceIn(1, FLUID_TANK_CAPACITY))
            }
        }
        contentVersion++
    }

    private fun contentsChanged() {
        contentVersion++
        setChanged()
    }

    private fun clearWithoutNotification() {
        itemTemplates.indices.forEach { slot ->
            itemTemplates[slot] = ItemStack.EMPTY
            itemCounts[slot] = 0
        }
        fluids.indices.forEach { tank -> fluids[tank] = FluidStack.EMPTY }
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
            if (stack.isEmpty || !isItemValid(slot, stack)) return stack

            val inserted = min(stack.count, ITEM_SLOT_CAPACITY - itemCounts[slot])
            if (inserted <= 0) return stack
            if (!simulate) {
                if (itemTemplates[slot].isEmpty) {
                    itemTemplates[slot] = stack.copyWithCount(1)
                }
                itemCounts[slot] += inserted
                contentsChanged()
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
                contentsChanged()
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
            val template = itemTemplates[slot]
            return !stack.isEmpty && (template.isEmpty || ItemStack.isSameItemSameComponents(template, stack))
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
            contentsChanged()
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
            var remaining = resource.amount
            val matching = fluids.indices.filter { tank ->
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
            if (action.execute() && filled > 0) contentsChanged()
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

            if (action.execute() && drained > 0) contentsChanged()
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

        internal const val ITEM_TOTAL_KEY = "ItemTotal"
        internal const val FLUID_TOTAL_KEY = "FluidTotal"

        private const val ITEMS_KEY = "Items"
        private const val FLUIDS_KEY = "Fluids"
        private const val SLOT_KEY = "Slot"
        private const val TANK_KEY = "Tank"
        private const val ITEM_KEY = "Item"
        private const val FLUID_KEY = "Fluid"
        private const val COUNT_KEY = "Count"
    }
}
