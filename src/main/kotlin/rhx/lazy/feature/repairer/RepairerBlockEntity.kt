package rhx.lazy.feature.repairer

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.MachineBlockEntity

internal class RepairerBlockEntity(
    pos: BlockPos,
    state: BlockState,
    private val itemRepairHook: ItemRepairHook = ItemRepairHooks,
) : MachineBlockEntity(RepairerRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private var storedItem = ItemStack.EMPTY

    val itemHandler: IItemHandlerModifiable = RepairerItemHandler()

    fun hasRepairableItem(): Boolean = isRepairable(storedItem)

    fun repairOnce(
        minimumRepairPercent: Int,
        maximumRepairPercent: Int,
        random: RandomSource,
        player: Player? = null,
    ): Boolean {
        if (!hasRepairableItem()) return false

        val percentRange = normalizedRepairPercentRange(minimumRepairPercent, maximumRepairPercent)
        val repairPercent = percentRange.first + random.nextInt(percentRange.last - percentRange.first + 1)
        val amount = repairAmount(storedItem.maxDamage, repairPercent)
        storedItem.damageValue = (storedItem.damageValue - amount).coerceAtLeast(0)
        itemRepairHook.afterRepair(storedItem, player)
        markDirty(STORED_ITEM_FIELD)
        return true
    }

    /** The item being repaired belongs to the player, not to the machine, so it drops on its own. */
    override fun takeHeldItems(): List<ItemStack> {
        if (storedItem.isEmpty) return emptyList()
        val dropped = storedItem.copy()
        clearStoredItem()
        return listOf(dropped)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        if (!storedItem.isEmpty && storedItem.count != 1) {
            storedItem = storedItem.copyWithCount(1)
        }
    }

    private fun setStoredItem(stack: ItemStack) {
        storedItem = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        markDirty(STORED_ITEM_FIELD)
    }

    private fun clearStoredItem() {
        setStoredItem(ItemStack.EMPTY)
    }

    private inner class RepairerItemHandler : IItemHandlerModifiable {
        override fun getSlots(): Int = SLOT_COUNT

        override fun getStackInSlot(slot: Int): ItemStack {
            validateSlot(slot)
            return if (storedItem.isEmpty) ItemStack.EMPTY else storedItem.copy()
        }

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            validateSlot(slot)
            if (!storedItem.isEmpty || !isItemValid(slot, stack)) return stack

            if (!simulate) {
                setStoredItem(stack)
            }
            return if (stack.count == 1) ItemStack.EMPTY else stack.copyWithCount(stack.count - 1)
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            validateSlot(slot)
            if (amount <= 0 || storedItem.isEmpty) return ItemStack.EMPTY

            val extracted = storedItem.copy()
            if (!simulate) {
                clearStoredItem()
            }
            return extracted
        }

        override fun getSlotLimit(slot: Int): Int {
            validateSlot(slot)
            return 1
        }

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean {
            validateSlot(slot)
            return isRepairable(stack)
        }

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) {
            validateSlot(slot)
            setStoredItem(stack)
        }
    }

    companion object {
        private const val SLOT_COUNT = 1
        private const val STORED_ITEM_FIELD = "storedItem"

        fun isRepairable(stack: ItemStack): Boolean =
            !stack.isEmpty &&
                stack.isDamageableItem &&
                stack.isDamaged

        internal fun normalizedRepairPercentRange(
            firstPercent: Int,
            secondPercent: Int,
        ): IntRange {
            val lower = minOf(firstPercent, secondPercent).coerceIn(MIN_REPAIR_PERCENT, MAX_REPAIR_PERCENT)
            val upper = maxOf(firstPercent, secondPercent).coerceIn(MIN_REPAIR_PERCENT, MAX_REPAIR_PERCENT)
            return lower..upper
        }

        internal fun repairAmount(
            maxDamage: Int,
            repairPercent: Int,
        ): Int {
            val safeMaxDamage = maxDamage.coerceAtLeast(1)
            val safePercent = repairPercent.coerceIn(MIN_REPAIR_PERCENT, MAX_REPAIR_PERCENT)
            return (
                (safeMaxDamage.toLong() * safePercent + PERCENT_DENOMINATOR - 1) /
                    PERCENT_DENOMINATOR
            ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }

        private fun validateSlot(slot: Int) {
            if (slot != 0) {
                throw IndexOutOfBoundsException("Slot $slot is out of range for repairer")
            }
        }

        private const val PERCENT_DENOMINATOR = 100L
        private const val MIN_REPAIR_PERCENT = 1
        private const val MAX_REPAIR_PERCENT = 100
    }
}
