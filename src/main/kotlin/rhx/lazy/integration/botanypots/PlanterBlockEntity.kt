package rhx.lazy.integration.botanypots

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil
import net.darkhax.botanypots.common.impl.BotanyPotsMod
import net.darkhax.botanypots.common.impl.Helpers
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.ManagedBlockEntity
import rhx.lazy.core.storage.NetworkStorage
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort

internal class PlanterBlockEntity(
    pos: BlockPos,
    state: BlockState,
    private val networkStorage: NetworkStoragePort = NetworkStorage,
) : ManagedBlockEntity(PlanterRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private val inputs = MutableList(INPUT_SLOT_COUNT) { ItemStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private val outputs = MutableList(OUTPUT_SLOT_COUNT) { ItemStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private val pendingDrops = mutableListOf<ItemStack>()

    @field:Persisted
    @field:LazyManaged
    private var growthTicks = 0f

    @field:Persisted
    @field:LazyManaged
    private var downwardOutputEnabled = false

    @field:Persisted
    @field:LazyManaged
    private var networkForwardingEnabled = false

    @field:Persisted
    @field:LazyManaged
    private var dimensionNetworkId = INVALID_NETWORK_ID

    private val recipeContext = PlanterBotanyContext(this)
    private val recipeResolver = PlanterRecipeResolver(recipeContext)
    private val outputRouter =
        PlanterOutputRouter(
            blockPos = pos,
            outputs = outputs,
            pendingDrops = pendingDrops,
            networkStorage = networkStorage,
            networkId = ::networkIdOrNull,
            disableNetworkForwarding = ::disableNetworkForwarding,
            markOutputsDirty = { markDirty(OUTPUTS_FIELD) },
            markPendingDirty = { markDirty(PENDING_DROPS_FIELD) },
        )

    internal val activeCrop: Crop?
        get() = recipeResolver.activeCrop

    internal val activeSoil: Soil?
        get() = recipeResolver.activeSoil

    val inputHandler: IItemHandlerModifiable = InputHandler()
    val outputHandler: IItemHandlerModifiable = outputRouter.outputHandler
    val bottomOutputHandler: IItemHandlerModifiable = outputRouter.outputHandler

    val isDownwardOutputEnabled: Boolean
        get() = downwardOutputEnabled

    val isNetworkForwardingEnabled: Boolean
        get() = networkForwardingEnabled

    val hasPendingDrops: Boolean
        get() = outputRouter.hasPendingDrops

    fun serverTick() {
        val level = level as? ServerLevel ?: return
        if (isRemoved) return

        routeStoredItems(level)
        if (outputRouter.hasPendingDrops) return

        val pot = insertedPotBlock() ?: return recipeResolver.clearActive()
        val soil = recipeResolver.resolveSoil(level)
        val crop = recipeResolver.resolveCrop(level)

        soil?.onTick(recipeContext, level)
        crop?.onTick(recipeContext, level)
        if (soil == null || crop == null) return
        if (!crop.isGrowthSustained(recipeContext, level)) return

        growthTicks += 1f
        crop.onGrowthTick(recipeContext, level)
        val requiredTicks = requiredGrowthTicks(crop, soil, pot)
        markDirty(GROWTH_TICKS_FIELD)
        if (growthTicks < requiredTicks || !crop.canHarvest(recipeContext, level)) return

        harvest(level, crop, soil, pot)
    }

    fun getInput(slot: Int): ItemStack {
        validateInputSlot(slot)
        val stack = inputs[slot]
        return if (stack.isEmpty) ItemStack.EMPTY else stack.copy()
    }

    fun progress(): Float {
        val required = requiredGrowthTicks()
        if (required <= 0) return 0f
        return (growthTicks / required.toFloat()).coerceIn(0f, 1f)
    }

    fun requiredGrowthTicks(): Int {
        val level = level ?: return -1
        val pot = insertedPotBlock() ?: return -1
        val soil = activeSoil ?: recipeResolver.resolveSoil(level) ?: return -1
        val crop = activeCrop ?: recipeResolver.resolveCrop(level) ?: return -1
        return requiredGrowthTicks(crop, soil, pot)
    }

    fun toggleDownwardOutput() {
        downwardOutputEnabled = !downwardOutputEnabled
        markDirty(DOWNWARD_OUTPUT_FIELD)
        if (downwardOutputEnabled) {
            (level as? ServerLevel)?.let(::routeStoredItems)
        }
    }

    fun enableNetworkForwarding(networkId: NetworkStorageId) {
        networkForwardingEnabled = true
        dimensionNetworkId = networkId.value
        markDirty(NETWORK_FORWARDING_FIELD)
        markDirty(DIMENSION_NETWORK_ID_FIELD)
        (level as? ServerLevel)?.let(::routeStoredItems)
    }

    fun disableNetworkForwarding() {
        if (!networkForwardingEnabled && dimensionNetworkId == INVALID_NETWORK_ID) return
        networkForwardingEnabled = false
        dimensionNetworkId = INVALID_NETWORK_ID
        markDirty(NETWORK_FORWARDING_FIELD)
        markDirty(DIMENSION_NETWORK_ID_FIELD)
    }

    fun pendingTooltipTag(): CompoundTag = outputRouter.pendingTooltipTag(level?.registryAccess())

    fun takeAllForDrop(): List<ItemStack> {
        val drops =
            buildList {
                inputs.filterNot(ItemStack::isEmpty).forEach { add(it.copy()) }
                addAll(outputRouter.takeAllForDrop())
            }
        repeat(inputs.size) { inputs[it] = ItemStack.EMPTY }
        growthTicks = 0f
        recipeResolver.invalidate()
        markDirty(INPUTS_FIELD)
        markDirty(GROWTH_TICKS_FIELD)
        return drops
    }

    fun runFunction(functionId: ResourceLocation) {
        val level = level as? ServerLevel ?: return
        val function = level.server.functions.get(functionId)
        val source = createCommandSource(level)
        function.ifPresentOrElse(
            { level.server.functions.execute(it, source) },
            { BotanyPotsMod.LOG.error("Planter at {} tried to run missing function {}", blockPos, functionId) },
        )
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        inputs.resize(INPUT_SLOT_COUNT)
        inputs.indices.forEach { slot ->
            inputs[slot] = normalizeSingle(inputs[slot])
        }
        outputRouter.normalizeAfterLoad()
        if (networkForwardingEnabled && dimensionNetworkId < 0) {
            networkForwardingEnabled = false
            dimensionNetworkId = INVALID_NETWORK_ID
        }
        recipeResolver.invalidate()
    }

    private fun harvest(
        level: ServerLevel,
        crop: Crop,
        soil: Soil,
        pot: BotanyPotBlock,
    ) {
        val rolls = Helpers.determineRollCount(totalYield(crop, soil, pot), level.random)
        repeat(rolls) {
            crop.onHarvest(recipeContext, level, outputRouter::enqueue)
        }
        routeStoredItems(level)
        growthTicks = 0f
        markDirty(GROWTH_TICKS_FIELD)
        level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(blockState))
    }

    private fun insertedPotBlock(): BotanyPotBlock? {
        val blockItem = inputs[POT_SLOT].item as? BlockItem ?: return null
        return blockItem.block as? BotanyPotBlock
    }

    private fun requiredGrowthTicks(
        crop: Crop,
        soil: Soil,
        pot: BotanyPotBlock,
    ): Int {
        val level = requireNotNull(level)
        val cropTime = crop.getRequiredGrowthTicks(recipeContext, level)
        var modifier =
            BotanyPotsMod.CONFIG
                .get()
                .gameplay.global_growth_modifier
        modifier += soil.getGrowthModifier(recipeContext, level)
        modifier += pot.getGrowthModifier(recipeContext, level, crop, soil)
        return Mth.floor(cropTime / modifier)
    }

    private fun totalYield(
        crop: Crop,
        soil: Soil,
        pot: BotanyPotBlock,
    ): Float {
        val level = requireNotNull(level)
        val scale = crop.getYieldScale(recipeContext, level)
        return crop.getBaseYield(recipeContext, level) +
            scale * soil.getYieldModifier(recipeContext, level) +
            scale * pot.getYieldModifier(recipeContext, level, crop, soil)
    }

    private fun routeStoredItems(level: ServerLevel) {
        outputRouter.route(
            level = level,
            networkForwardingEnabled = networkForwardingEnabled,
            downwardOutputEnabled = downwardOutputEnabled,
        )
    }

    private fun networkIdOrNull(): NetworkStorageId? =
        if (networkForwardingEnabled && dimensionNetworkId >= 0) {
            NetworkStorageId(dimensionNetworkId)
        } else {
            null
        }

    private fun createCommandSource(level: ServerLevel): CommandSourceStack {
        val name = Component.translatable("block.lazy.planter")
        return CommandSourceStack(
            CommandSource.NULL,
            Vec3.atCenterOf(blockPos),
            Vec2.ZERO,
            level,
            FUNCTION_PERMISSION_LEVEL,
            name.string,
            name,
            level.server,
            null,
        )
    }

    private fun setInput(
        slot: Int,
        stack: ItemStack,
    ) {
        validateInputSlot(slot)
        val normalized = normalizeSingle(stack)
        if (ItemStack.matches(inputs[slot], normalized)) return
        inputs[slot] = normalized
        growthTicks = 0f
        recipeResolver.invalidate()
        markDirty(INPUTS_FIELD)
        markDirty(GROWTH_TICKS_FIELD)
    }

    private fun isValidInput(
        slot: Int,
        stack: ItemStack,
    ): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            POT_SLOT -> (stack.item as? BlockItem)?.block is BotanyPotBlock
            SOIL_SLOT -> level?.let { recipeResolver.isValidSoil(it, stack) } == true
            SEED_SLOT -> level?.let { recipeResolver.isValidCrop(it, stack) } == true
            else -> false
        }
    }

    private inner class InputHandler : IItemHandlerModifiable {
        override fun getSlots(): Int = INPUT_SLOT_COUNT

        override fun getStackInSlot(slot: Int): ItemStack = getInput(slot)

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack {
            validateInputSlot(slot)
            if (!inputs[slot].isEmpty || !isItemValid(slot, stack)) return stack
            if (!simulate) setInput(slot, stack)
            return if (stack.count == 1) ItemStack.EMPTY else stack.copyWithCount(stack.count - 1)
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            validateInputSlot(slot)
            val stored = inputs[slot]
            if (amount <= 0 || stored.isEmpty) return ItemStack.EMPTY
            val result = stored.copy()
            if (!simulate) setInput(slot, ItemStack.EMPTY)
            return result
        }

        override fun getSlotLimit(slot: Int): Int {
            validateInputSlot(slot)
            return 1
        }

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean {
            validateInputSlot(slot)
            return isValidInput(slot, stack)
        }

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) {
            if (stack.isEmpty || isValidInput(slot, stack)) {
                setInput(slot, stack)
            }
        }
    }

    companion object {
        const val POT_SLOT = 0
        const val SOIL_SLOT = 1
        const val SEED_SLOT = 2
        const val INPUT_SLOT_COUNT = 3
        const val OUTPUT_SLOT_COUNT = PlanterOutputRouter.OUTPUT_SLOT_COUNT

        private const val INPUTS_FIELD = "inputs"
        private const val OUTPUTS_FIELD = "outputs"
        private const val PENDING_DROPS_FIELD = "pendingDrops"
        private const val GROWTH_TICKS_FIELD = "growthTicks"
        private const val DOWNWARD_OUTPUT_FIELD = "downwardOutputEnabled"
        private const val NETWORK_FORWARDING_FIELD = "networkForwardingEnabled"
        private const val DIMENSION_NETWORK_ID_FIELD = "dimensionNetworkId"
        private const val INVALID_NETWORK_ID = -1
        private const val FUNCTION_PERMISSION_LEVEL = 2

        private fun validateInputSlot(slot: Int) {
            if (slot !in 0 until INPUT_SLOT_COUNT) {
                throw IndexOutOfBoundsException("Input slot $slot is out of range for planter")
            }
        }

        private fun normalizeSingle(stack: ItemStack): ItemStack = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
    }
}

private fun MutableList<ItemStack>.resize(size: Int) {
    while (this.size > size) {
        removeAt(lastIndex)
    }
    while (this.size < size) {
        add(ItemStack.EMPTY)
    }
}
