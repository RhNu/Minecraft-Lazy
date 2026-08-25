package rhx.lazy.integration.mysticalagriculture

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.IItemHandlerModifiable
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.StoredOutputSource
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ResourceItemHandler
import rhx.lazy.core.resource.ResourceKinds
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.core.resource.itemAmount
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class EssenceConverterBlockEntity(
    pos: BlockPos,
    state: BlockState,
    private val capacityProvider: () -> Long = { EssenceConverterConfigs.settings.maxStoredEssence.get() },
) : IoManagedBlockEntity(EssenceConverterRegistries.blockEntity.get(), pos, state) {
    private var selectedTierName = NO_TARGET

    private var conversionRemainder = 0

    private val outputStore = ResourceStore(ItemResourceKind, 1, Long.MAX_VALUE, ::outputChanged)
    private val outputSource = StoredOutputSource(listOf(outputStore))

    val inputHandler: IItemHandlerModifiable = InputHandler()
    val outputHandler: IItemHandlerModifiable = ResourceItemHandler(outputStore, allowInsert = false)
    val combinedHandler: IItemHandlerModifiable = CombinedInvWrapper(inputHandler, outputHandler)

    private var stateRepairPendingPersistence = false

    init {
        installIoAdapter(EssenceIoAdapter())
    }

    val targetTier: EssenceTier?
        get() = EssenceTier.fromSerializedName(selectedTierName)

    val outputCount: Long
        get() = outputStore.amount(0)

    val remainderUnits: Int
        get() = conversionRemainder

    val isNetworkOutputPaused: Boolean
        get() = ioController.networkPaused

    val capacity: Long
        get() = capacityProvider().coerceAtLeast(1L)

    fun hasContents(): Boolean = currentLedger().hasContents

    override fun hasStoredContents(): Boolean = hasContents()

    fun selectTarget(tier: EssenceTier): Boolean {
        if (!tier.isAvailable()) return false
        val changed = currentLedger().withTarget(tier) ?: return false
        applyLedger(changed)
        return true
    }

    fun clearContents(): Boolean {
        if (!hasContents()) return false
        applyLedger(currentLedger().clear())
        return true
    }

    fun onServerTick() {
        persistStateRepairs()
        normalizeState()
        ioController.tick()
    }

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        if (selectedTierName != NO_TARGET) tag.putString(TARGET_TIER_FIELD, selectedTierName)
        if (conversionRemainder > 0) tag.putInt(STORED_REMAINDER_FIELD, conversionRemainder)
        if (!outputStore.isEmpty) tag.put(OUTPUT_STORE_TAG, outputStore.save(registries))
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        selectedTierName = tag.getString(TARGET_TIER_FIELD)
        conversionRemainder = tag.getInt(STORED_REMAINDER_FIELD).coerceAtLeast(0)
        outputStore.load(registries, tag.getList(OUTPUT_STORE_TAG, Tag.TAG_COMPOUND.toInt()))
        stateRepairPendingPersistence = normalizeState(markChanges = false)
    }

    private fun normalizeState(markChanges: Boolean = true): Boolean {
        val current = currentLedger()
        val normalized = current.downgradeMissingInsanium(capacity)
        applyLedger(normalized, markChanges)
        return current != normalized
    }

    private fun currentLedger(): EssenceLedger =
        EssenceLedger(
            target = targetTier,
            outputCount = outputCount,
            remainderUnits = conversionRemainder,
        )

    private fun applyLedger(
        ledger: EssenceLedger,
        markChanges: Boolean = true,
    ) {
        val targetName = ledger.target?.serializedName ?: NO_TARGET
        val targetChanged = selectedTierName != targetName
        val remainderChanged = conversionRemainder != ledger.remainderUnits
        selectedTierName = targetName
        conversionRemainder = ledger.remainderUnits
        val replacement =
            ledger.target
                ?.takeIf { ledger.outputCount > 0L }
                ?.createStack()
                ?.let { itemAmount(it, ledger.outputCount) }
        outputStore.replace(0, replacement, notify = markChanges)
        if (!markChanges) return
        if (targetChanged || remainderChanged) setChanged()
    }

    private fun outputChanged() {
        setChanged()
    }

    private fun persistStateRepairs() {
        if (!stateRepairPendingPersistence) return
        stateRepairPendingPersistence = false
        setChanged()
    }

    private inner class EssenceIoAdapter : IoAdapter {
        override val capabilities = setOf(ResourceKinds.ITEM)
        override val outputSource = this@EssenceConverterBlockEntity.outputSource
    }

    private inner class InputHandler : IItemHandlerModifiable {
        override fun getSlots(): Int = 1

        override fun getStackInSlot(slot: Int): ItemStack {
            validateSlot(slot)
            return ItemStack.EMPTY
        }

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            validateSlot(slot)
            val tier = EssenceTier.fromStack(stack) ?: return stack
            val insertion = currentLedger().insert(tier, stack.count, capacity)
            if (insertion.accepted <= 0) return stack
            if (!simulate) applyLedger(insertion.ledger)
            return if (insertion.accepted == stack.count) ItemStack.EMPTY else stack.copyWithCount(stack.count - insertion.accepted)
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            validateSlot(slot)
            return ItemStack.EMPTY
        }

        override fun getSlotLimit(slot: Int): Int {
            validateSlot(slot)
            return Int.MAX_VALUE
        }

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean {
            validateSlot(slot)
            return EssenceTier.fromStack(stack) != null
        }

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) {
            validateSlot(slot)
            if (stack.isEmpty || !insertItem(slot, stack, true).isEmpty) return
            insertItem(slot, stack, false)
        }
    }

    companion object {
        public const val TARGET_TIER_FIELD = "selectedTier"
        public const val STORED_REMAINDER_FIELD = "conversionRemainder"
        public const val OUTPUT_STORE_TAG = "essenceOutput"
        private const val NO_TARGET = ""

        private fun validateSlot(slot: Int) {
            if (slot != 0) throw IndexOutOfBoundsException("Essence Converter slot $slot is out of range")
        }
    }
}
