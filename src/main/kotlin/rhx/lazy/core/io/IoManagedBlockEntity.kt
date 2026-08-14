package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.ManagedBlockEntity

internal abstract class IoManagedBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : ManagedBlockEntity(type, pos, state) {
    private var ioConfiguration = IoConfiguration.DEFAULT
    private var ioNetworkPaused = false

    val ioController = IoController(this)

    protected fun installIoAdapter(adapter: IoAdapter): IoController {
        ioController.install(adapter)
        return ioController
    }

    protected fun stripIoConfiguration(tag: CompoundTag) {
        tag.remove(IO_CONFIGURATION_TAG)
        tag.remove(IO_NETWORK_PAUSED_TAG)
    }

    internal fun storedIoConfiguration(): IoConfiguration = ioConfiguration.copy(networkTarget = ioConfiguration.networkTarget?.copy())

    internal fun storedIoNetworkPaused(): Boolean = ioNetworkPaused

    internal fun updateIoConfiguration(configuration: IoConfiguration) {
        val copied = configuration.copy(networkTarget = configuration.networkTarget?.copy())
        if (ioConfiguration == copied) return
        ioConfiguration = copied
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
        tag.put(IO_CONFIGURATION_TAG, ioConfiguration.save())
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
        ioNetworkPaused = tag.getBoolean(IO_NETWORK_PAUSED_TAG)
        ioController.normalize()
    }

    private companion object {
        const val IO_CONFIGURATION_TAG = "lazyIoConfiguration"
        const val IO_NETWORK_PAUSED_TAG = "lazyIoNetworkPaused"
    }
}

internal interface IoConfigurationEditor {
    val configuration: IoConfiguration
    val capabilities: Set<NetworkInsertCapability>
    val networkPaused: Boolean
        get() = false

    fun setMode(mode: IoMode)

    fun cycleSide(side: RelativeSide)

    fun toggleAutoEject()

    fun setNetworkTarget(target: NetworkTargetRef): Boolean
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

    internal fun install(newAdapter: IoAdapter) {
        adapter = newAdapter
        normalize()
    }

    internal fun normalize() {
        val current = configuration
        val normalizedSides = RelativeSide.entries.associateWith(current::side)
        if (current.sides != normalizedSides) {
            update(current.copy(sides = normalizedSides))
        }
        if (current.mode != IoMode.NETWORK) {
            blockEntity.updateIoNetworkPaused(false)
        }
    }

    override fun setMode(mode: IoMode) {
        update(configuration.copy(mode = mode))
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
    }

    override fun cycleSide(side: RelativeSide) {
        val current = configuration
        update(current.withSide(side, current.side(side).next()))
    }

    override fun toggleAutoEject() {
        val current = configuration
        update(current.copy(autoEject = !current.autoEject))
    }

    override fun setNetworkTarget(target: NetworkTargetRef): Boolean {
        val currentAdapter = adapter ?: return false
        if (!currentAdapter.supportsNetworkTarget(target)) return false
        update(configuration.copy(mode = IoMode.NETWORK, networkTarget = target.copy()))
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
        return true
    }

    fun applyConfiguration(configuration: IoConfiguration): Boolean {
        val target = configuration.networkTarget
        if (configuration.mode == IoMode.NETWORK && target != null) {
            val provider = NetworkOutputProviders.get(target.providerId)
            if (provider != null && !checkNotNull(adapter).supportsNetworkTarget(target)) return false
        }
        update(configuration)
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
        return true
    }

    fun sideMode(direction: Direction?): SideIoMode {
        if (direction == null) return SideIoMode.BOTH
        return when (configuration.mode) {
            IoMode.PASSIVE -> SideIoMode.BOTH
            IoMode.FACE -> configuration.side(RelativeSide.fromWorldDirection(blockEntity.blockState, direction))
            IoMode.NETWORK -> SideIoMode.INPUT
        }
    }

    fun outputDirections(): Set<Direction> =
        if (configuration.mode != IoMode.FACE || !configuration.autoEject) {
            emptySet()
        } else {
            RelativeSide.entries
                .filter { configuration.side(it).allowsOutput }
                .mapTo(linkedSetOf()) { it.toWorldDirection(blockEntity.blockState) }
        }

    fun tick() {
        val currentAdapter = adapter ?: return
        val current = configuration
        if (current.mode == IoMode.PASSIVE && !currentAdapter.ticksWhenPassive) return
        if (retryTicks > 0) {
            retryTicks--
            return
        }
        if (current.mode == IoMode.NETWORK && (current.networkTarget == null || networkPaused)) return

        when (currentAdapter.push(current)) {
            IoPushResult.Success -> retryTicks = 0
            IoPushResult.Retry -> retryTicks = NETWORK_RETRY_INTERVAL - 1
            IoPushResult.TargetMissing -> {
                setMode(IoMode.PASSIVE)
            }
            IoPushResult.OutcomeUnknown -> {
                blockEntity.updateIoNetworkPaused(true)
                retryTicks = 0
            }
        }
    }

    private fun update(configuration: IoConfiguration) {
        blockEntity.updateIoConfiguration(configuration)
    }

    private companion object {
        const val NETWORK_RETRY_INTERVAL = 20
    }
}
