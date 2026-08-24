package rhx.lazy.feature.simulation

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.event.EventHooks
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.ResourceKinds
import rhx.lazy.core.io.StoredOutputSource
import rhx.lazy.core.process.PreparedCommit
import rhx.lazy.core.process.WorkController
import rhx.lazy.core.process.WorkProvider
import rhx.lazy.core.process.WorkStatus
import rhx.lazy.core.process.WorkStep
import rhx.lazy.core.render.MachineActivity
import rhx.lazy.core.render.MachineDisplayState
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ResourceFluidHandler
import rhx.lazy.core.resource.ResourceItemHandler
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.feature.machine.ProcessingCoreRegistries
import rhx.lazy.integration.api.LazyInternalApi
import kotlin.math.min

@LazyInternalApi
public class SimulationChamberBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(SimulationRegistries.blockEntity.get(), pos, state) {
    private val inputs = MutableList(INPUT_SLOTS) { ItemStack.EMPTY }

    private val itemOutputs = ResourceStore(ItemResourceKind, OUTPUT_ENTRIES, Long.MAX_VALUE, ::outputsChanged)
    private val fluidOutputs = ResourceStore(FluidResourceKind, OUTPUT_ENTRIES, Long.MAX_VALUE, ::outputsChanged)
    private val outputSource = StoredOutputSource(listOf(itemOutputs, fluidOutputs))

    private var activeJob: SimulationJob? = null

    val inputItemHandler: IItemHandlerModifiable = SimulationInputHandler(inputs, ::inputLimit, ::validInput, ::setInput)
    val outputItemHandler = ResourceItemHandler(itemOutputs, allowInsert = false)
    val outputFluidHandler: IFluidHandler = ResourceFluidHandler(fluidOutputs, allowInsert = false)

    private val workController =
        WorkController(
            provider =
                object : WorkProvider {
                    override fun step(workBudget: Int): WorkStep = produceStep(workBudget)

                    override fun committed(workUnits: Int) = commitWorkUnits(workUnits)
                },
            commit = { prepared ->
                val complete = prepared.drainInto(itemOutputs, fluidOutputs)
                setChanged()
                complete
            },
        )

    init {
        installIoAdapter(OutputIoAdapter())
    }

    fun serverTick() {
        val serverLevel = level as? ServerLevel ?: return
        val ioCycle = ioController.beginTick()
        if (activeJob == null && workController.preparedCommit == null) startJob(serverLevel)
        advanceJob(serverLevel)
        ioController.endTick(ioCycle)
        tickDisplayState()
    }

    fun progress(): Float {
        val job = activeJob ?: return 0f
        return (job.progressTicks.toFloat() / job.duration).coerceIn(0f, 1f)
    }

    fun speedMultiplier(): Int =
        activeJob?.speedMultiplier
            ?: ProcessingCoreRegistries.tier(inputs[CORE_SLOT])?.simulationSpeedMultiplier()
            ?: 0

    fun outputMultiplier(): Long =
        activeJob?.outputMultiplier
            ?: ProcessingCoreRegistries
                .tier(inputs[CORE_SLOT])
                ?.let { tier ->
                    Math.multiplyExact(inputs[CORE_SLOT].count.toLong(), tier.simulationOutputMultiplier().toLong())
                }
            ?: 0L

    fun hasWaitingOutputs(): Boolean = workController.status == WorkStatus.BLOCKED

    fun getInput(slot: Int): ItemStack = inputs[slot].copy()

    override fun computeDisplayState(): MachineDisplayState {
        val target = activeJob?.target ?: inputs[TARGET_SLOT]
        val icon = SimulationDisplayIcons.iconFor(target)
        if (icon.isEmpty) return MachineDisplayState.EMPTY
        return MachineDisplayState(icon, displayActivity())
    }

    private fun displayActivity(): MachineActivity =
        when {
            workController.status == WorkStatus.BLOCKED || workController.status == WorkStatus.FAULTED -> MachineActivity.BLOCKED
            activeJob != null -> MachineActivity.RUNNING
            else -> MachineActivity.IDLE
        }

    fun hasContents(): Boolean =
        inputs.any { !it.isEmpty } ||
            !itemOutputs.isEmpty ||
            !fluidOutputs.isEmpty ||
            activeJob != null ||
            workController.preparedCommit != null

    override fun hasStoredContents(): Boolean = hasContents()

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        tag.put(
            INPUT_STORE_TAG,
            ListTag().apply {
                inputs.forEachIndexed { slot, stack ->
                    if (!stack.isEmpty) {
                        add(
                            CompoundTag().apply {
                                putInt(SLOT_TAG, slot)
                                put(STACK_TAG, stack.save(registries))
                            },
                        )
                    }
                }
            },
        )
        if (!itemOutputs.isEmpty) tag.put(ITEM_OUTPUT_STORE_TAG, itemOutputs.save(registries))
        if (!fluidOutputs.isEmpty) tag.put(FLUID_OUTPUT_STORE_TAG, fluidOutputs.save(registries))
        activeJob?.let { tag.put(ACTIVE_JOB_TAG, it.save(registries)) }
        workController.preparedCommit?.let { tag.put(PREPARED_COMMIT_TAG, it.save(registries)) }
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        inputs.indices.forEach { inputs[it] = ItemStack.EMPTY }
        tag.getList(INPUT_STORE_TAG, Tag.TAG_COMPOUND.toInt()).forEach { raw ->
            val entry = raw as? CompoundTag ?: return@forEach
            val slot = entry.getInt(SLOT_TAG)
            if (slot in inputs.indices) inputs[slot] = ItemStack.parseOptional(registries, entry.getCompound(STACK_TAG))
        }
        itemOutputs.load(registries, tag.getList(ITEM_OUTPUT_STORE_TAG, Tag.TAG_COMPOUND.toInt()))
        fluidOutputs.load(registries, tag.getList(FLUID_OUTPUT_STORE_TAG, Tag.TAG_COMPOUND.toInt()))
        activeJob =
            tag
                .takeIf { it.contains(ACTIVE_JOB_TAG, Tag.TAG_COMPOUND.toInt()) }
                ?.getCompound(ACTIVE_JOB_TAG)
                ?.let { SimulationJob.parse(registries, it) }
        val prepared =
            tag
                .takeIf { activeJob != null && it.contains(PREPARED_COMMIT_TAG, Tag.TAG_COMPOUND.toInt()) }
                ?.getCompound(PREPARED_COMMIT_TAG)
                ?.let { PreparedCommit.parse(registries, it) }
        workController.restore(prepared)
    }

    private fun startJob(level: ServerLevel) {
        val simulation = SimulationRecipeResolver.resolve(level, inputs[TARGET_SLOT]) ?: return
        val tier = ProcessingCoreRegistries.tier(inputs[CORE_SLOT]) ?: return
        val rolls = Math.multiplyExact(inputs[CORE_SLOT].count.toLong(), tier.simulationOutputMultiplier().toLong())
        if (rolls <= 0L) return
        activeJob =
            SimulationJob(
                target = inputs[TARGET_SLOT],
                batch = SimulationBatch.from(simulation, rolls),
                duration = simulation.duration,
                speedMultiplier = tier.simulationSpeedMultiplier(),
                outputMultiplier = rolls,
                tools = inputs.subList(TOOL_SLOT_START, INPUT_SLOTS),
            )
        setChanged()
        refreshDisplayState()
    }

    private fun advanceJob(level: ServerLevel) {
        val job = activeJob ?: return
        if (job.progressTicks < job.duration) {
            job.progressTicks = min(job.duration, Math.addExact(job.progressTicks, job.speedMultiplier))
            setChanged()
            if (job.progressTicks < job.duration) return
        }
        workController.tick(SimulationConfigs.settings.rollBudgetPerTick.get())
    }

    private fun produceStep(workBudget: Int): WorkStep {
        val level = level as? ServerLevel ?: return WorkStep.Idle
        val job = activeJob ?: return WorkStep.Idle
        if (job.progressTicks < job.duration || workBudget <= 0) return WorkStep.Running
        val units = min(job.batch.remaining, workBudget.toLong()).toInt()
        if (units <= 0) return WorkStep.Idle
        val loadout = SimulationToolModules.loadout(job.tools)
        val accumulator = SimulationOutputAccumulator(loadout::acceptsOutput)
        return try {
            when (val batch = job.batch) {
                is SimulationBatch.Item -> {
                    repeat(units) {
                        batch.blockLootOutputs.forEach { output ->
                            net.minecraft.world.level.block.Block
                                .getDrops(output.state, level, blockPos, null, null, output.tool)
                                .forEach(accumulator::add)
                        }
                    }
                    rollOutputs(level.random, batch.itemOutputs, batch.fluidOutputs, units, accumulator)
                }
                is SimulationBatch.Entity -> {
                    val type =
                        BuiltInRegistries.ENTITY_TYPE.getOptional(batch.entityId).orElse(null)
                            ?: return WorkStep.Faulted("Missing entity type ${batch.entityId}")
                    if (!EntitySimulationTargets.isAllowed(
                            type,
                        )
                    ) {
                        return WorkStep.Faulted("Entity type ${batch.entityId} is no longer allowed")
                    }
                    repeat(units) {
                        if (batch.rollLootTable) {
                            val entity =
                                createTemporaryEntity(type, level)
                                    ?: return WorkStep.Faulted("Could not create entity ${batch.entityId}")
                            SimulationLootRoller.roll(level, entity, batch.lootTable.orElse(null), loadout.weapon, accumulator::add)
                        }
                    }
                    rollOutputs(level.random, batch.itemOutputs, batch.fluidOutputs, units, accumulator)
                }
            }
            WorkStep.Produced(accumulator.prepare(units))
        } catch (overflow: ArithmeticException) {
            WorkStep.Faulted("Simulation output overflow: ${overflow.message.orEmpty()}")
        }
    }

    private fun commitWorkUnits(workUnits: Int) {
        val job = activeJob ?: return
        val remaining = job.batch.remaining - workUnits
        if (remaining <= 0L) {
            activeJob = null
            workController.clear()
            refreshDisplayState()
        } else {
            job.batch = job.batch.withRemaining(remaining)
        }
        setChanged()
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
                    accumulator.add(output.stack, Math.multiplyExact(output.stack.count.toLong(), rolls.toLong()))
                }
            }
        }
        fluidOutputs.forEach { output ->
            repeat(budget) {
                if (random.nextFloat() < output.chance) {
                    val rolls = random.nextIntBetweenInclusive(output.minRolls, output.maxRolls)
                    accumulator.add(output.stack, Math.multiplyExact(output.stack.amount.toLong(), rolls.toLong()))
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
                val isolatedLevel = IsolatedServerLevelAccessor(level)
                try {
                    EventHooks.finalizeMobSpawn(
                        entity,
                        isolatedLevel,
                        level.getCurrentDifficultyAt(blockPos),
                        MobSpawnType.MOB_SUMMONED,
                        null,
                    )
                } finally {
                    isolatedLevel.discardCompanions(entity)
                }
            }
        }

    private fun setInput(
        slot: Int,
        stack: ItemStack,
    ) {
        if (ItemStack.matches(inputs[slot], stack)) return
        inputs[slot] = if (stack.isEmpty) ItemStack.EMPTY else stack.copy()
        setChanged()
        if (activeJob == null) refreshDisplayState()
    }

    private fun inputLimit(
        slot: Int,
        stack: ItemStack,
    ): Int = if (slot == CORE_SLOT) min(64, stack.maxStackSize.coerceAtLeast(1)) else 1

    private fun validInput(
        slot: Int,
        stack: ItemStack,
    ): Boolean =
        when (slot) {
            TARGET_SLOT -> level?.let { SimulationRecipeResolver.resolve(it, stack) } != null
            CORE_SLOT -> ProcessingCoreRegistries.tier(stack) != null
            in TOOL_SLOT_START until INPUT_SLOTS -> SimulationToolModules.claims(stack)
            else -> false
        }

    private fun outputsChanged() {
        setChanged()
    }

    private inner class OutputIoAdapter : IoAdapter {
        override val capabilities = setOf(ResourceKinds.ITEM, ResourceKinds.FLUID)
        override val outputSource = this@SimulationChamberBlockEntity.outputSource
    }

    companion object {
        const val TARGET_SLOT = 0
        const val CORE_SLOT = 1
        const val TOOL_SLOT_START = 2
        const val TOOL_SLOTS = 3
        const val INPUT_SLOTS = TOOL_SLOT_START + TOOL_SLOTS
        const val OUTPUT_ENTRIES = 28

        private const val INPUT_STORE_TAG = "simulationInputs"
        private const val ITEM_OUTPUT_STORE_TAG = "simulationItemOutput"
        private const val FLUID_OUTPUT_STORE_TAG = "simulationFluidOutput"
        private const val ACTIVE_JOB_TAG = "simulationJob"
        private const val PREPARED_COMMIT_TAG = "preparedCommit"
        private const val SLOT_TAG = "slot"
        private const val STACK_TAG = "stack"
    }
}

internal fun simulationRollBudget(
    remaining: Long,
    configuredLimit: Int,
): Int = if (remaining <= 0L) 0 else min(remaining, configuredLimit.coerceAtLeast(1).toLong()).toInt()
