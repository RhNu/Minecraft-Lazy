package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.MachineBlockEntity
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public abstract class IoManagedBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : MachineBlockEntity(type, pos, state) {
    private var ioConfiguration = IoConfiguration.DEFAULT
    private var ioNetworkPaused = false

    val ioController = IoController(this)

    protected fun installIoAdapter(adapter: IoAdapter) {
        ioController.install(adapter)
    }

    /**
     * The live configuration. Callers must treat it — and the opaque network target inside it — as
     * read-only; [updateIoConfiguration] is the only way to change it.
     */
    public fun storedIoConfiguration(): IoConfiguration = ioConfiguration

    public fun storedIoNetworkPaused(): Boolean = ioNetworkPaused

    public fun updateIoConfiguration(configuration: IoConfiguration) {
        if (ioConfiguration == configuration) return
        ioConfiguration = configuration
        setChanged()
        invalidateCapabilities()
    }

    public fun updateIoNetworkPaused(paused: Boolean) {
        if (ioNetworkPaused == paused) return
        ioNetworkPaused = paused
        setChanged()
    }

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        if (!ioConfiguration.isDefault) tag.put(IO_CONFIGURATION_TAG, ioConfiguration.save())
        if (ioNetworkPaused) tag.putBoolean(IO_NETWORK_PAUSED_TAG, true)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        ioConfiguration =
            if (tag.contains(IO_CONFIGURATION_TAG, Tag.TAG_COMPOUND.toInt())) {
                IoConfiguration.load(tag.getCompound(IO_CONFIGURATION_TAG))
            } else {
                IoConfiguration.DEFAULT
            }
        ioNetworkPaused = ioConfiguration.mode == IoMode.NETWORK && tag.getBoolean(IO_NETWORK_PAUSED_TAG)
        ioController.resetBackoff()
        invalidateCapabilities()
    }

    override fun settingKeys(): Set<String> = setOf(IO_CONFIGURATION_TAG, IO_NETWORK_PAUSED_TAG)

    override fun setRemoved() {
        ioController.invalidateOutputs()
        super.setRemoved()
    }

    private companion object {
        const val IO_CONFIGURATION_TAG = "lazyIoConfiguration"
        const val IO_NETWORK_PAUSED_TAG = "lazyIoNetworkPaused"
    }
}

@LazyInternalApi
public interface IoConfigurationEditor {
    val configuration: IoConfiguration
    val capabilities: Set<ResourceKind<out ResourceVariant>>

    /** False for machines that can only emit, so cycling a face skips the input-capable states. */
    val acceptsInput: Boolean
        get() = true

    val networkPaused: Boolean
        get() = false

    fun setMode(mode: IoMode)

    fun cycleSide(side: RelativeSide)

    fun toggleAutoEject()

    fun setNetworkTarget(target: NetworkTargetRef): Boolean

    fun clearNetworkTarget()

    fun resumeNetwork() = Unit
}

@LazyInternalApi
public class IoController(
    private val blockEntity: IoManagedBlockEntity,
) : IoConfigurationEditor {
    private var adapter: IoAdapter? = null
    private var retryTicks = 0
    private val outputDispatcher = OutputDispatcher(blockEntity.blockPos) { !blockEntity.isRemoved }

    override val configuration: IoConfiguration
        get() = blockEntity.storedIoConfiguration()

    val mode: IoMode
        get() = configuration.mode

    override val networkPaused: Boolean
        get() = blockEntity.storedIoNetworkPaused()

    val target: NetworkTargetRef?
        get() = configuration.networkTarget

    override val capabilities: Set<ResourceKind<out ResourceVariant>>
        get() = adapter?.capabilities ?: emptySet()

    override val acceptsInput: Boolean
        get() = adapter?.acceptsInput != false

    public fun install(newAdapter: IoAdapter) {
        adapter = newAdapter
    }

    public fun resetBackoff() {
        retryTicks = 0
    }

    public fun invalidateOutputs() = outputDispatcher.invalidate()

    override fun setMode(mode: IoMode) {
        update(configuration.copy(mode = mode))
    }

    override fun cycleSide(side: RelativeSide) {
        val current = configuration
        update(current.withSide(side, current.side(side).next(acceptsInput)))
    }

    override fun toggleAutoEject() {
        val current = configuration
        update(current.copy(autoEject = !current.autoEject))
    }

    override fun setNetworkTarget(target: NetworkTargetRef): Boolean {
        if (adapter?.supportsNetworkTarget(target) != true) return false
        update(configuration.copy(mode = IoMode.NETWORK, networkTarget = target.deepCopy()))
        return true
    }

    override fun clearNetworkTarget() {
        update(configuration.copy(mode = IoMode.PASSIVE, networkTarget = null))
    }

    override fun resumeNetwork() {
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
    }

    /**
     * Copies a full template onto this machine. A network target whose provider is currently absent
     * is preserved so the binding survives a temporarily missing mod; one the machine can never use
     * is rejected outright.
     */
    fun applyConfiguration(configuration: IoConfiguration): Boolean {
        val target = configuration.networkTarget
        if (configuration.mode == IoMode.NETWORK && target != null && NetworkOutputProviders.get(target.providerId) != null) {
            if (adapter?.supportsNetworkTarget(target) != true) return false
        }
        update(configuration.deepCopy())
        return true
    }

    fun sideMode(direction: Direction?): SideIoMode {
        if (direction == null) return SideIoMode.BOTH
        val current = configuration
        return when (current.mode) {
            IoMode.PASSIVE -> SideIoMode.BOTH
            IoMode.FACE -> current.side(RelativeSide.fromWorldDirection(blockEntity.blockState, direction))
            IoMode.NETWORK -> SideIoMode.INPUT
        }
    }

    fun outputDirections(): Set<Direction> {
        val current = configuration
        if (current.mode != IoMode.FACE || !current.autoEject) return emptySet()
        return RelativeSide.entries
            .filter { current.side(it).allowsOutput }
            .mapTo(linkedSetOf()) { it.toWorldDirection(blockEntity.blockState) }
    }

    /** A one-pass convenience for sources with no work phase, such as buffers and infinite sources. */
    fun tick() {
        beginTick()
    }

    /**
     * Starts a work/IO cycle by outputting old contents. [endTick] reuses the same transfer budget
     * after work, so pre- and post-output together can never exceed the per-tick offer bound.
     */
    internal fun beginTick(): IoTransferCycle? {
        val currentAdapter = adapter ?: return null
        val current = configuration
        val source = currentAdapter.outputSource
        val budget = TransferBudget()
        val cycle = IoTransferCycle(current.mode, source, current.networkTarget, outputDirections(), budget)
        when (current.mode) {
            IoMode.PASSIVE -> return cycle
            IoMode.FACE -> {
                val directions = cycle.directions
                if (directions.isEmpty()) return cycle
                if (!readyToPush(currentAdapter)) return cycle
                cycle.active = handle(dispatch(cycle))
            }
            IoMode.NETWORK -> {
                val target = current.networkTarget ?: return cycle
                if (networkPaused) return cycle
                if (!readyToPush(currentAdapter)) return cycle
                cycle.active = handle(dispatch(cycle))
            }
        }
        return cycle
    }

    internal fun endTick(cycle: IoTransferCycle?) {
        cycle ?: return
        if (!cycle.active || cycle.budget.exhausted || cycle.source == null) return
        cycle.active = handle(dispatch(cycle))
    }

    private fun dispatch(cycle: IoTransferCycle): IoPushResult {
        return when (cycle.mode) {
            IoMode.PASSIVE -> IoPushResult.Success
            IoMode.FACE -> {
                val serverLevel = blockEntity.level as? net.minecraft.server.level.ServerLevel ?: return IoPushResult.Retry
                outputDispatcher.pushToFaces(serverLevel, requireNotNull(cycle.source), cycle.directions, cycle.budget)
            }
            IoMode.NETWORK ->
                outputDispatcher.pushToNetwork(
                    requireNotNull(cycle.source),
                    requireNotNull(cycle.target),
                    cycle.budget,
                )
        }
    }

    private fun readyToPush(adapter: IoAdapter): Boolean {
        if (retryTicks > 0) {
            retryTicks--
            return false
        }
        return adapter.readyToPush()
    }

    private fun handle(result: IoPushResult): Boolean {
        when (result) {
            IoPushResult.Success -> retryTicks = 0
            IoPushResult.Retry -> retryTicks = NETWORK_RETRY_INTERVAL - 1
            IoPushResult.TargetMissing -> clearNetworkTarget()
            IoPushResult.OutcomeUnknown -> {
                blockEntity.updateIoNetworkPaused(true)
                retryTicks = 0
            }
        }
        return result == IoPushResult.Success
    }

    private fun update(configuration: IoConfiguration) {
        blockEntity.updateIoConfiguration(configuration)
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
    }

    private companion object {
        const val NETWORK_RETRY_INTERVAL = 20
    }
}

internal class IoTransferCycle public constructor(
    public val mode: IoMode,
    public val source: OutputSource?,
    public val target: NetworkTargetRef?,
    public val directions: Set<Direction>,
    public val budget: TransferBudget,
) {
    public var active: Boolean = false
}
