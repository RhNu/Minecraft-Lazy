package rhx.lazy.feature.simulation

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.event.EventHooks
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.ManagedBlockEntity.Companion.MANAGED_DATA_KEY
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NeighborCapabilities
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.storage.LongItemStack
import kotlin.math.min

internal class SimulationChamberBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(SimulationRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private val inputs = MutableList(INPUT_SLOTS) { ItemStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private val outputs = MutableList(SimulationOutputRouter.ITEM_SLOTS) { ItemStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private val fluids = MutableList(SimulationOutputRouter.FLUID_TANKS) { FluidStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private var progressTicks = 0

    private val pendingItems = mutableListOf<LongItemStack>()
    private val pendingFluids = mutableListOf<LongFluidStack>()
    private var batch: SimulationBatch? = null
    private var legacyBatch: LegacyBatch? = null
    private val neighborItems = NeighborCapabilities.items(blockPos) { !isRemoved }
    private val neighborFluids = NeighborCapabilities.fluids(blockPos) { !isRemoved }
    private val outputRouter =
        SimulationOutputRouter(outputs, fluids, pendingItems, pendingFluids, neighborItems, neighborFluids) {
            setChanged()
        }

    val inputItemHandler: IItemHandlerModifiable = SimulationInputHandler(inputs, ::inputLimit, ::validInput, ::setInput)
    val outputItemHandler: IItemHandlerModifiable = outputRouter.itemHandler
    val outputFluidHandler: IFluidHandler = outputRouter.fluidHandler

    init {
        installIoAdapter(OutputIoAdapter())
    }

    fun serverTick() {
        val level = level as? ServerLevel ?: return
        migrateLegacyBatch(level)
        advance(level)
        ioController.tick()
    }

    /**
     * The chamber refuses to start while anything is still buffered, so [serverTick] runs this first
     * and hands the results to the IO cycle within the same tick: a batch that finished never spends
     * a tick reported as blocked, and the next recipe starts on the very next tick.
     */
    private fun advance(level: ServerLevel) {
        if (batch != null) return processBatch(level)
        if (outputRouter.hasOutputs) return
        val simulation = SimulationRecipeResolver.resolve(level, inputs[TARGET_SLOT]) ?: return resetProgress()
        val tier = SimulationRegistries.coreTier(inputs[CORE_SLOT]) ?: return resetProgress()
        progressTicks += tier.speedMultiplier()
        markDirty(PROGRESS_FIELD)
        if (progressTicks < simulation.duration) return
        beginBatch(simulation, tier)
        processBatch(level)
    }

    /** A batch that spans several ticks holds the bar full: the recipe is done and only rolls remain. */
    fun progress(): Float {
        if (batch != null) return 1f
        val simulation = level?.let { SimulationRecipeResolver.resolve(it, inputs[TARGET_SLOT]) } ?: return 0f
        return (progressTicks.toFloat() / simulation.duration).coerceIn(0f, 1f)
    }

    fun speedMultiplier(): Int = SimulationRegistries.coreTier(inputs[CORE_SLOT])?.speedMultiplier() ?: 0

    fun outputMultiplier(): Long =
        SimulationRegistries
            .coreTier(inputs[CORE_SLOT])
            ?.let { tier -> inputs[CORE_SLOT].count.toLong() * tier.outputMultiplier() }
            ?: 0L

    fun hasWaitingOutputs(): Boolean = batch == null && outputRouter.hasOutputs

    fun getInput(slot: Int): ItemStack = inputs[slot].copy()

    fun hasContents(): Boolean = inputs.any { !it.isEmpty } || outputRouter.hasOutputs || batch != null || legacyBatch != null

    override fun hasStoredContents(): Boolean = hasContents()

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        if (pendingItems.isNotEmpty()) {
            tag.put(PENDING_ITEMS, ListTag().apply { pendingItems.forEach { add(it.save(registries)) } })
        }
        if (pendingFluids.isNotEmpty()) {
            tag.put(PENDING_FLUIDS, ListTag().apply { pendingFluids.forEach { add(it.save(registries)) } })
        }
        batch?.let { tag.put(BATCH_TAG, it.save(registries)) }
        legacyBatch?.save(tag, registries)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        inputs.resize(INPUT_SLOTS) { ItemStack.EMPTY }
        neighborItems.invalidate()
        neighborFluids.invalidate()
        outputRouter.normalize()
        pendingItems.clear()
        tag.getList(PENDING_ITEMS, Tag.TAG_COMPOUND.toInt()).forEach { raw ->
            LongItemStack.parse(registries, raw as CompoundTag)?.let(pendingItems::add)
        }
        pendingFluids.clear()
        tag.getList(PENDING_FLUIDS, Tag.TAG_COMPOUND.toInt()).forEach { raw ->
            LongFluidStack.parse(registries, raw as CompoundTag)?.let(pendingFluids::add)
        }
        batch =
            if (tag.contains(BATCH_TAG, Tag.TAG_COMPOUND.toInt())) {
                SimulationBatch.parse(registries, tag.getCompound(BATCH_TAG))
            } else {
                null
            }
        legacyBatch = if (batch == null) LegacyBatch.parse(tag, registries) else null
    }

    override fun setRemoved() {
        neighborItems.invalidate()
        neighborFluids.invalidate()
        super.setRemoved()
    }

    private fun migrateLegacyBatch(level: ServerLevel) {
        val legacy = legacyBatch ?: return
        legacyBatch = null
        batch =
            when (legacy.kind) {
                LEGACY_ITEM_KIND -> {
                    val id = ResourceLocation.tryParse(legacy.recipeId) ?: return markBatchDirty()
                    val holder =
                        level.recipeManager
                            .getAllRecipesFor(SimulationRegistries.itemRecipeType.get())
                            .firstOrNull { it.id() == id } ?: return markBatchDirty()
                    val recipe = holder.value()
                    SimulationBatch.from(
                        ResolvedSimulation.Item(
                            holder.id(),
                            recipe.durationTicks(),
                            recipe.itemOutputs,
                            recipe.fluidOutputs,
                        ),
                        legacy.remaining,
                    )
                }
                LEGACY_AUTOMATIC_KIND ->
                    legacy.automaticOutput
                        .takeUnless(ItemStack::isEmpty)
                        ?.let {
                            SimulationBatch.Item(
                                listOf(SimulationItemOutput(it)),
                                emptyList(),
                                emptyList(),
                                legacy.remaining,
                            )
                        }
                LEGACY_ENTITY_KIND -> {
                    val entityId = ResourceLocation.tryParse(legacy.entityId) ?: return markBatchDirty()
                    val profile =
                        level.recipeManager
                            .getAllRecipesFor(SimulationRegistries.entityRecipeType.get())
                            .firstOrNull { it.id().toString() == legacy.recipeId }
                    if (!legacy.recipeId.startsWith("lazy:entity/") && profile == null) return markBatchDirty()
                    SimulationBatch.from(
                        ResolvedSimulation.EntityProfile(entityId, profile, SimulationConfigs.settings.defaultDuration.get()),
                        legacy.remaining,
                    )
                }
                else -> null
            }
        markBatchDirty()
    }

    private fun beginBatch(
        simulation: ResolvedSimulation,
        tier: SimulationCoreTier,
    ) {
        val rolls = inputs[CORE_SLOT].count.toLong() * tier.outputMultiplier().toLong()
        batch = SimulationBatch.from(simulation, rolls)
        progressTicks = 0
        markBatchDirty()
    }

    private fun processBatch(level: ServerLevel) {
        val active = batch ?: return
        val budget =
            min(
                active.remaining,
                SimulationConfigs.settings.maxRollsPerTick
                    .get()
                    .toLong(),
            ).toInt()
        val accumulator = SimulationOutputAccumulator()
        when (active) {
            is SimulationBatch.Item -> {
                repeat(budget) {
                    active.blockLootOutputs.forEach { output ->
                        net.minecraft.world.level.block.Block
                            .getDrops(output.state, level, blockPos, null, null, output.tool)
                            .forEach(accumulator::add)
                    }
                }
                rollOutputs(level.random, active.itemOutputs, active.fluidOutputs, budget, accumulator)
            }
            is SimulationBatch.Entity -> {
                val type = BuiltInRegistries.ENTITY_TYPE.getOptional(active.entityId).orElse(null) ?: return cancelBatch()
                if (type.`is`(SimulationTags.dataModelBlacklist)) return cancelBatch()
                repeat(budget) {
                    if (active.rollLootTable) {
                        val entity = createTemporaryEntity(type, level) ?: return cancelBatch()
                        SimulationLootRoller.roll(level, entity, active.lootTable.orElse(null), accumulator::add)
                    }
                }
                rollOutputs(level.random, active.itemOutputs, active.fluidOutputs, budget, accumulator)
            }
        }
        accumulator.flush(outputRouter)
        val remaining = active.remaining - budget
        if (remaining <= 0) cancelBatch() else batch = active.withRemaining(remaining).also { setChanged() }
    }

    private fun rollOutputs(
        random: RandomSource,
        itemOutputs: List<SimulationItemOutput>,
        fluidOutputs: List<SimulationFluidOutput>,
        budget: Int,
        accumulator: SimulationOutputAccumulator,
    ) {
        itemOutputs.forEach { output ->
            repeat(budget) {
                if (random.nextFloat() < output.chance) {
                    val rolls = random.nextIntBetweenInclusive(output.minRolls, output.maxRolls)
                    accumulator.add(output.stack, output.stack.count.toLong() * rolls)
                }
            }
        }
        fluidOutputs.forEach { output ->
            repeat(budget) {
                if (random.nextFloat() < output.chance) {
                    val rolls = random.nextIntBetweenInclusive(output.minRolls, output.maxRolls)
                    accumulator.add(output.stack, output.stack.amount.toLong() * rolls)
                }
            }
        }
    }

    private fun createTemporaryEntity(
        type: EntityType<*>,
        level: ServerLevel,
    ): LivingEntity? =
        (type.create(level) as? LivingEntity)?.also { entity ->
            entity.setPos(blockPos.center)
            if (entity is Mob) {
                EventHooks.finalizeMobSpawn(entity, level, level.getCurrentDifficultyAt(blockPos), MobSpawnType.MOB_SUMMONED, null)
            }
        }

    private fun resetProgress() {
        if (progressTicks == 0) return
        progressTicks = 0
        markDirty(PROGRESS_FIELD)
    }

    private fun cancelBatch() {
        batch = null
        markBatchDirty()
    }

    private fun markBatchDirty() {
        markDirty(PROGRESS_FIELD)
        setChanged()
    }

    private fun setInput(
        slot: Int,
        stack: ItemStack,
    ) {
        if (ItemStack.matches(inputs[slot], stack)) return
        inputs[slot] = stack
        if (slot == TARGET_SLOT) resetProgress()
        markDirty(INPUTS_FIELD)
    }

    private fun inputLimit(
        slot: Int,
        stack: ItemStack,
    ) = if (slot == TARGET_SLOT) 1 else min(64, stack.maxStackSize.coerceAtLeast(1))

    private fun validInput(
        slot: Int,
        stack: ItemStack,
    ): Boolean =
        when (slot) {
            TARGET_SLOT -> level?.let { SimulationRecipeResolver.resolve(it, stack) } != null
            CORE_SLOT -> SimulationRegistries.coreTier(stack) != null
            else -> false
        }

    private inner class OutputIoAdapter : IoAdapter {
        override val capabilities = setOf(NetworkInsertCapabilities.ITEM, NetworkInsertCapabilities.FLUID)
        override val maintainsWhenIdle = true

        override fun maintain() = outputRouter.movePendingLocal()

        override fun pushToFaces(directions: Set<Direction>): IoPushResult {
            val serverLevel = level as? ServerLevel ?: return IoPushResult.Retry
            return outputRouter.pushToFaces(serverLevel, directions)
        }

        override fun pushToNetwork(target: NetworkTargetRef): IoPushResult = outputRouter.pushToNetwork(target)
    }

    companion object {
        const val TARGET_SLOT = 0
        const val CORE_SLOT = 1
        const val INPUT_SLOTS = 2
        private const val INPUTS_FIELD = "inputs"
        private const val PROGRESS_FIELD = "progressTicks"
        private const val BATCH_TAG = "lazySimulationBatch"
        private const val PENDING_ITEMS = "lazyPendingItems"
        private const val PENDING_FLUIDS = "lazyPendingFluids"
        private const val LEGACY_KIND_FIELD = "batchKind"
        private const val LEGACY_ID_FIELD = "batchId"
        private const val LEGACY_ENTITY_FIELD = "batchEntity"
        private const val LEGACY_REMAINING_FIELD = "batchRemaining"
        private const val LEGACY_AUTOMATIC_OUTPUT_FIELD = "batchAutomaticOutput"
        private const val LEGACY_ITEM_KIND = "item"
        private const val LEGACY_AUTOMATIC_KIND = "automatic"
        private const val LEGACY_ENTITY_KIND = "entity"
    }

    private data class LegacyBatch(
        val kind: String,
        val recipeId: String,
        val entityId: String,
        val remaining: Long,
        val automaticOutput: ItemStack,
    ) {
        fun save(
            root: CompoundTag,
            registries: HolderLookup.Provider,
        ) {
            val managed = root.getCompound(MANAGED_DATA_KEY)
            managed.putString(LEGACY_KIND_FIELD, kind)
            managed.putString(LEGACY_ID_FIELD, recipeId)
            managed.putString(LEGACY_ENTITY_FIELD, entityId)
            managed.putLong(LEGACY_REMAINING_FIELD, remaining)
            if (!automaticOutput.isEmpty) managed.put(LEGACY_AUTOMATIC_OUTPUT_FIELD, automaticOutput.save(registries))
            root.put(MANAGED_DATA_KEY, managed)
        }

        companion object {
            fun parse(
                root: CompoundTag,
                registries: HolderLookup.Provider,
            ): LegacyBatch? {
                val managed = root.getCompound(MANAGED_DATA_KEY)
                val remaining = managed.getLong(LEGACY_REMAINING_FIELD)
                if (remaining <= 0L) return null
                return LegacyBatch(
                    managed.getString(LEGACY_KIND_FIELD),
                    managed.getString(LEGACY_ID_FIELD),
                    managed.getString(LEGACY_ENTITY_FIELD),
                    remaining,
                    ItemStack.parseOptional(registries, managed.getCompound(LEGACY_AUTOMATIC_OUTPUT_FIELD)),
                )
            }
        }
    }
}

private fun <T> MutableList<T>.resize(
    size: Int,
    factory: () -> T,
) {
    while (this.size > size) removeAt(lastIndex)
    while (this.size < size) add(factory())
}
