package rhx.lazy.integration.mysticalagriculture

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import net.neoforged.neoforge.items.ItemHandlerHelper
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.IoRoute
import rhx.lazy.core.io.IoRouteAdapter
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputRouter
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTransferResult

internal class EssenceConverterBlockEntity(
    pos: BlockPos,
    state: BlockState,
    private val capacityProvider: () -> Long = { EssenceConverterConfigs.settings.maxStoredEssence.get() },
) : IoManagedBlockEntity(EssenceConverterRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private var targetTierName = NO_TARGET

    @field:Persisted
    @field:LazyManaged
    private var storedOutput = 0L

    @field:Persisted
    @field:LazyManaged
    private var storedRemainder = 0

    val inputHandler: IItemHandlerModifiable = InputHandler()
    val outputHandler: IItemHandlerModifiable = OutputHandler()
    val combinedHandler: IItemHandlerModifiable = CombinedInvWrapper(inputHandler, outputHandler)

    private var downwardCache: BlockCapabilityCache<IItemHandler, Direction?>? = null
    private var stateRepairPendingPersistence = false

    private val ioAdapter = EssenceIoRouteAdapter()

    init {
        installIoAdapter(ioAdapter)
    }

    val targetTier: EssenceTier?
        get() = EssenceTier.fromSerializedName(targetTierName)

    val outputCount: Long
        get() = storedOutput

    val remainderUnits: Int
        get() = storedRemainder

    val isNetworkOutputPaused: Boolean
        get() = ioController.networkPaused

    val capacity: Long
        get() = capacityProvider().coerceAtLeast(1L)

    fun hasContents(): Boolean = currentLedger().hasContents

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

    fun saveContentsToItem(
        stack: ItemStack,
        registries: HolderLookup.Provider,
    ) {
        saveToItem(stack, registries)
        val data = stack.get(DataComponents.BLOCK_ENTITY_DATA) ?: return
        val root = data.copyTag()
        stripIoConfiguration(root)
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(root))
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        downwardCache = null
        stateRepairPendingPersistence = normalizeState(markChanges = false)
    }

    override fun setRemoved() {
        downwardCache = null
        super.setRemoved()
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
            outputCount = storedOutput,
            remainderUnits = storedRemainder,
        )

    private fun applyLedger(
        ledger: EssenceLedger,
        markChanges: Boolean = true,
    ) {
        val targetName = ledger.target?.serializedName ?: NO_TARGET
        val targetChanged = targetTierName != targetName
        val outputChanged = storedOutput != ledger.outputCount
        val remainderChanged = storedRemainder != ledger.remainderUnits
        targetTierName = targetName
        storedOutput = ledger.outputCount
        storedRemainder = ledger.remainderUnits
        if (!markChanges) return
        if (targetChanged) markDirty(TARGET_TIER_FIELD)
        if (outputChanged) markDirty(STORED_OUTPUT_FIELD)
        if (remainderChanged) markDirty(STORED_REMAINDER_FIELD)
    }

    private fun pushDownward(): IoPushResult {
        val serverLevel = level as? ServerLevel ?: return IoPushResult.Retry
        val target = downwardCache(serverLevel).getCapability() ?: return IoPushResult.Success
        val tier = targetTier ?: return IoPushResult.Success
        val offered = storedOutput.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (offered <= 0) return IoPushResult.Success
        val stack = tier.createStack(offered)
        if (stack.isEmpty) return IoPushResult.Success
        val inserted = EssenceOutputPusher.pushMaximum(target, stack)
        if (inserted > 0) applyLedger(currentLedger().removeOutput(inserted.toLong()))
        return IoPushResult.Success
    }

    private fun pushToNetwork(target: NetworkTargetRef?): IoPushResult {
        val networkTarget = target ?: return IoPushResult.TargetMissing
        val tier = targetTier ?: return IoPushResult.Success
        val template = tier.createStack()
        if (template.isEmpty || storedOutput <= 0L) return IoPushResult.Success
        val result = NetworkOutputRouter.insert(networkTarget, NetworkPayload.Items(template, storedOutput), false)
        return when (result) {
            is NetworkTransferResult.Success -> {
                val remainder = result.remainder.coerceIn(0L, storedOutput)
                val inserted = storedOutput - remainder
                if (inserted > 0L) applyLedger(currentLedger().removeOutput(inserted))
                IoPushResult.Success
            }

            NetworkTransferResult.TargetMissing -> IoPushResult.TargetMissing
            NetworkTransferResult.InvalidTarget -> IoPushResult.TargetMissing
            NetworkTransferResult.OutcomeUnknown -> IoPushResult.OutcomeUnknown
            NetworkTransferResult.TemporarilyUnavailable -> IoPushResult.Retry
        }
    }

    private fun downwardCache(level: ServerLevel): BlockCapabilityCache<IItemHandler, Direction?> =
        downwardCache
            ?: BlockCapabilityCache
                .create<IItemHandler, Direction?>(
                    Capabilities.ItemHandler.BLOCK,
                    level,
                    blockPos.below(),
                    Direction.UP,
                    { !isRemoved },
                    {},
                ).also { downwardCache = it }

    private fun persistStateRepairs() {
        if (!stateRepairPendingPersistence) return
        stateRepairPendingPersistence = false
        markDirty(TARGET_TIER_FIELD)
        markDirty(STORED_OUTPUT_FIELD)
        markDirty(STORED_REMAINDER_FIELD)
    }

    private inner class EssenceIoRouteAdapter : IoRouteAdapter {
        override val supportedRoutes: Set<IoRoute> =
            setOf(IoRoute.PASSIVE, IoRoute.DOWNWARD, IoRoute.NETWORK)
        override val capabilities = setOf(NetworkInsertCapabilities.ITEM)

        override fun push(
            route: IoRoute,
            target: NetworkTargetRef?,
        ): IoPushResult =
            when (route) {
                IoRoute.DOWNWARD -> pushDownward()
                IoRoute.NETWORK -> pushToNetwork(target)
                else -> IoPushResult.Success
            }
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
            return if (insertion.accepted == stack.count) {
                ItemStack.EMPTY
            } else {
                stack.copyWithCount(stack.count - insertion.accepted)
            }
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

    private inner class OutputHandler : IItemHandlerModifiable {
        override fun getSlots(): Int = 1

        override fun getStackInSlot(slot: Int): ItemStack {
            validateSlot(slot)
            val tier = targetTier ?: return ItemStack.EMPTY
            val count = storedOutput.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            return if (count <= 0) ItemStack.EMPTY else tier.createStack(count)
        }

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            validateSlot(slot)
            return stack
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            validateSlot(slot)
            val tier = targetTier ?: return ItemStack.EMPTY
            val template = tier.createStack()
            if (template.isEmpty) return ItemStack.EMPTY
            val extraction = currentLedger().extract(amount, template.maxStackSize.coerceAtLeast(1))
            if (extraction.extracted <= 0) return ItemStack.EMPTY
            if (!simulate) applyLedger(extraction.ledger)
            return template.copyWithCount(extraction.extracted)
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
            return false
        }

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) {
            validateSlot(slot)
        }
    }

    companion object {
        internal const val MANAGED_DATA_KEY = "managed"
        internal const val TARGET_TIER_FIELD = "targetTierName"
        internal const val STORED_OUTPUT_FIELD = "storedOutput"
        internal const val STORED_REMAINDER_FIELD = "storedRemainder"
        private const val NO_TARGET = ""

        private fun validateSlot(slot: Int) {
            if (slot != 0) throw IndexOutOfBoundsException("Essence Converter slot $slot is out of range")
        }
    }
}

internal object EssenceOutputPusher {
    fun pushMaximum(
        target: IItemHandler,
        stack: ItemStack,
    ): Int {
        if (stack.isEmpty) return 0
        val remainder = ItemHandlerHelper.insertItemStacked(target, stack, false)
        return stack.count - remainder.count.coerceIn(0, stack.count)
    }
}
