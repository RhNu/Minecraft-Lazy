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
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.IoRoute
import rhx.lazy.core.io.IoRouteAdapter
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkTargetRef

internal class PlanterBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(PlanterRegistries.blockEntity.get(), pos, state) {
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

    private var renderSeed = ItemStack.EMPTY

    private val recipeContext = PlanterBotanyContext(this)
    private val recipeResolver = PlanterRecipeResolver(recipeContext)
    private val outputRouter =
        PlanterOutputRouter(
            blockPos = pos,
            outputs = outputs,
            pendingDrops = pendingDrops,
            markOutputsDirty = { markDirty(OUTPUTS_FIELD) },
            markPendingDirty = { markDirty(PENDING_DROPS_FIELD) },
        )

    private val ioAdapter = PlanterIoRouteAdapter()

    init {
        installIoAdapter(ioAdapter)
    }

    internal val activeCrop: Crop?
        get() = recipeResolver.activeCrop

    internal val activeSoil: Soil?
        get() = recipeResolver.activeSoil

    val inputHandler: IItemHandlerModifiable = InputHandler()
    val outputHandler: IItemHandlerModifiable = outputRouter.outputHandler
    val bottomOutputHandler: IItemHandlerModifiable = outputRouter.outputHandler

    val isDownwardOutputEnabled: Boolean
        get() = ioController.route == IoRoute.DOWNWARD

    val hasPendingDrops: Boolean
        get() = outputRouter.hasPendingDrops

    /**
     * The stacked pot count is the planter's output multiplier. Expose the effective value so
     * integrations do not need to reach into the input inventory directly.
     */
    val totalPotBonus: Int
        get() = inputs[POT_SLOT].count.coerceAtLeast(0)

    val seedForRendering: ItemStack
        get() = renderSeed

    fun serverTick() {
        val level = level as? ServerLevel ?: return
        if (isRemoved) return

        routeStoredItems()
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
        syncRenderSeed()
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
            inputs[slot] = normalizeInput(slot, inputs[slot])
        }
        outputRouter.normalizeAfterLoad()
        recipeResolver.invalidate()
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        CompoundTag().apply {
            val seed = inputs[SEED_SLOT]
            if (!seed.isEmpty) {
                put(RENDER_SEED_TAG, seed.save(registries))
            }
        }

    override fun handleUpdateTag(
        tag: CompoundTag,
        lookupProvider: HolderLookup.Provider,
    ) {
        renderSeed = normalizeInput(SEED_SLOT, ItemStack.parseOptional(lookupProvider, tag.getCompound(RENDER_SEED_TAG)))
    }

    override fun onDataPacket(
        connection: Connection,
        packet: ClientboundBlockEntityDataPacket,
        lookupProvider: HolderLookup.Provider,
    ) {
        handleUpdateTag(packet.tag, lookupProvider)
    }

    private fun harvest(
        level: ServerLevel,
        crop: Crop,
        soil: Soil,
        pot: BotanyPotBlock,
    ) {
        val rolls = Helpers.determineRollCount(totalYield(crop, soil, pot), level.random)
        val potCount = inputs[POT_SLOT].count.coerceAtLeast(1)
        repeat(rolls) {
            crop.onHarvest(recipeContext, level) { stack -> outputRouter.enqueue(stack, potCount) }
        }
        routeStoredItems()
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

    private fun routeStoredItems() {
        ioController.tick()
    }

    private inner class PlanterIoRouteAdapter : IoRouteAdapter {
        override val supportedRoutes: Set<IoRoute> =
            setOf(IoRoute.PASSIVE, IoRoute.DOWNWARD, IoRoute.NETWORK)
        override val capabilities = setOf(NetworkInsertCapabilities.ITEM)
        override val ticksWhenPassive: Boolean = true

        override fun push(
            route: IoRoute,
            target: NetworkTargetRef?,
        ): IoPushResult = outputRouter.route(level as? ServerLevel, route, target)
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
        val normalized = normalizeInput(slot, stack)
        if (ItemStack.matches(inputs[slot], normalized)) return
        inputs[slot] = normalized
        growthTicks = 0f
        recipeResolver.invalidate()
        markDirty(INPUTS_FIELD)
        markDirty(GROWTH_TICKS_FIELD)
        if (slot == SEED_SLOT) {
            syncRenderSeed()
        }
    }

    private fun syncRenderSeed() {
        val level = level ?: return
        if (!level.isClientSide) {
            level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
        }
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
            if (!isItemValid(slot, stack)) return stack

            val stored = inputs[slot]
            if (!stored.isEmpty && (slot != POT_SLOT || !ItemStack.isSameItemSameComponents(stored, stack))) {
                return stack
            }

            val limit = minOf(getSlotLimit(slot), stack.maxStackSize.coerceAtLeast(1))
            val inserted = minOf(stack.count, limit - stored.count)
            if (inserted <= 0) return stack
            if (!simulate) {
                val updated =
                    if (stored.isEmpty) {
                        stack.copyWithCount(inserted)
                    } else {
                        stored.copyWithCount(stored.count + inserted)
                    }
                setInput(slot, updated)
            }
            return if (inserted == stack.count) ItemStack.EMPTY else stack.copyWithCount(stack.count - inserted)
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack {
            validateInputSlot(slot)
            val stored = inputs[slot]
            if (amount <= 0 || stored.isEmpty) return ItemStack.EMPTY
            val extracted = minOf(amount, stored.count)
            val result = stored.copyWithCount(extracted)
            if (!simulate) {
                val remaining = stored.count - extracted
                setInput(slot, if (remaining == 0) ItemStack.EMPTY else stored.copyWithCount(remaining))
            }
            return result
        }

        override fun getSlotLimit(slot: Int): Int {
            validateInputSlot(slot)
            return if (slot == POT_SLOT) POT_SLOT_LIMIT else 1
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
            validateInputSlot(slot)
            if (!stack.isEmpty && !isValidInput(slot, stack)) return
            setInput(slot, stack)
        }
    }

    private fun normalizeInput(
        slot: Int,
        stack: ItemStack,
    ): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY
        val limit =
            if (slot == POT_SLOT) {
                minOf(POT_SLOT_LIMIT, stack.maxStackSize.coerceAtLeast(1))
            } else {
                1
            }
        return stack.copyWithCount(minOf(stack.count.coerceAtLeast(1), limit))
    }

    companion object {
        const val POT_SLOT = 0
        const val SOIL_SLOT = 1
        const val SEED_SLOT = 2
        const val INPUT_SLOT_COUNT = 3
        const val OUTPUT_SLOT_COUNT = PlanterOutputRouter.OUTPUT_SLOT_COUNT

        private const val POT_SLOT_LIMIT = 64
        private const val INPUTS_FIELD = "inputs"
        private const val OUTPUTS_FIELD = "outputs"
        private const val PENDING_DROPS_FIELD = "pendingDrops"
        private const val GROWTH_TICKS_FIELD = "growthTicks"
        private const val RENDER_SEED_TAG = "render_seed"
        private const val FUNCTION_PERMISSION_LEVEL = 2

        private fun validateInputSlot(slot: Int) {
            if (slot !in 0 until INPUT_SLOT_COUNT) {
                throw IndexOutOfBoundsException("Input slot $slot is out of range for planter")
            }
        }
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
