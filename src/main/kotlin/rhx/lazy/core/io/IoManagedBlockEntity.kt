package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.MachineBlockEntity

internal abstract class IoManagedBlockEntity(
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
    internal fun storedIoConfiguration(): IoConfiguration = ioConfiguration

    internal fun storedIoNetworkPaused(): Boolean = ioNetworkPaused

    internal fun updateIoConfiguration(configuration: IoConfiguration) {
        if (ioConfiguration == configuration) return
        ioConfiguration = configuration
        setChanged()
        invalidateCapabilities()
    }

    internal fun updateIoNetworkPaused(paused: Boolean) {
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

    private companion object {
        const val IO_CONFIGURATION_TAG = "lazyIoConfiguration"
        const val IO_NETWORK_PAUSED_TAG = "lazyIoNetworkPaused"
    }
}

internal interface IoConfigurationEditor {
    val configuration: IoConfiguration
    val capabilities: Set<NetworkInsertCapability>

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

internal class IoController(
    private val blockEntity: IoManagedBlockEntity,
) : IoConfigurationEditor {
    private var adapter: IoAdapter? = null
    private var retryTicks = 0

    override val configuration: IoConfiguration
        get() = blockEntity.storedIoConfiguration()

    val mode: IoMode
        get() = configuration.mode

    override val networkPaused: Boolean
        get() = blockEntity.storedIoNetworkPaused()

    val target: NetworkTargetRef?
        get() = configuration.networkTarget

    override val capabilities: Set<NetworkInsertCapability>
        get() = adapter?.capabilities ?: emptySet()

    override val acceptsInput: Boolean
        get() = adapter?.acceptsInput != false

    internal fun install(newAdapter: IoAdapter) {
        adapter = newAdapter
    }

    internal fun resetBackoff() {
        retryTicks = 0
    }

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

    /**
     * One IO cycle. Machines run it once per tick and *after* their own work, so whatever they
     * produced during the tick leaves in the same tick instead of spending one looking blocked.
     *
     * Maintenance runs whatever the mode is, because a buffered adapter has to keep draining even
     * when the mode's push is skipped — which a network machine does while it is unbound, paused or
     * inside its retry back-off.
     */
    fun tick() {
        val currentAdapter = adapter ?: return
        if (currentAdapter.maintainsWhenIdle) currentAdapter.maintain()
        val current = configuration
        when (current.mode) {
            IoMode.PASSIVE -> Unit
            IoMode.FACE -> {
                val directions = outputDirections()
                if (directions.isEmpty()) return
                if (!readyToPush(currentAdapter)) return
                handle(currentAdapter.pushToFaces(directions))
            }
            IoMode.NETWORK -> {
                val target = current.networkTarget ?: return
                if (networkPaused) return
                if (!readyToPush(currentAdapter)) return
                handle(currentAdapter.pushToNetwork(target))
            }
        }
    }

    private fun readyToPush(adapter: IoAdapter): Boolean {
        if (retryTicks > 0) {
            retryTicks--
            return false
        }
        return adapter.readyToPush()
    }

    private fun handle(result: IoPushResult) {
        when (result) {
            IoPushResult.Success -> retryTicks = 0
            IoPushResult.Retry -> retryTicks = NETWORK_RETRY_INTERVAL - 1
            IoPushResult.TargetMissing -> clearNetworkTarget()
            IoPushResult.OutcomeUnknown -> {
                blockEntity.updateIoNetworkPaused(true)
                retryTicks = 0
            }
        }
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
