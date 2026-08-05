package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.ManagedBlockEntity

internal abstract class IoManagedBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : ManagedBlockEntity(type, pos, state) {
    @field:Persisted
    @field:LazyManaged
    private var ioRouteName = IoRoute.PASSIVE.name

    @field:Persisted
    @field:LazyManaged
    private var ioProviderName = ""

    @field:Persisted
    @field:LazyManaged
    private var ioNetworkPaused = false

    private var ioTargetData = CompoundTag()
    private var cachedIoRoute = IoRoute.PASSIVE
    private var cachedIoProviderId: ResourceLocation? = null
    private var cachedIoTarget: NetworkTargetRef? = null

    val ioController = IoRouteController(this)

    protected fun installIoAdapter(adapter: IoRouteAdapter): IoRouteController {
        ioController.install(adapter)
        return ioController
    }

    protected fun stripIoConfiguration(tag: CompoundTag) {
        val managed = tag.getCompound(MANAGED_DATA_KEY)
        managed.remove(IO_ROUTE_FIELD)
        managed.remove(IO_PROVIDER_FIELD)
        managed.remove(IO_NETWORK_PAUSED_FIELD)
        tag.remove(IO_TARGET_DATA_TAG)
        tag.put(MANAGED_DATA_KEY, managed)
    }

    internal fun storedIoRoute(): IoRoute = cachedIoRoute

    internal fun hasCanonicalStoredIoRoute(): Boolean = ioRouteName == cachedIoRoute.name

    internal fun storedIoTarget(): NetworkTargetRef? = cachedIoTarget

    internal fun storedIoNetworkPaused(): Boolean = ioNetworkPaused

    internal fun updateIoRoute(route: IoRoute) {
        if (cachedIoRoute == route && ioRouteName == route.name) return
        ioRouteName = route.name
        cachedIoRoute = route
        markDirty(IO_ROUTE_FIELD)
    }

    internal fun updateIoProvider(target: NetworkTargetRef?) {
        val providerId = target?.providerId
        val targetData = target?.data?.copy() ?: CompoundTag()
        val providerName = providerId?.toString() ?: ""
        if (cachedIoProviderId == providerId && ioProviderName == providerName && ioTargetData == targetData) return
        ioProviderName = providerName
        ioTargetData = targetData
        cachedIoProviderId = providerId
        cachedIoTarget = providerId?.let { NetworkTargetRef(it, ioTargetData) }
        markDirty(IO_PROVIDER_FIELD)
    }

    internal fun updateIoNetworkPaused(paused: Boolean) {
        if (ioNetworkPaused == paused) return
        ioNetworkPaused = paused
        markDirty(IO_NETWORK_PAUSED_FIELD)
    }

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        if (!ioTargetData.isEmpty) {
            tag.put(IO_TARGET_DATA_TAG, ioTargetData.copy())
        }
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        ioTargetData =
            if (tag.contains(IO_TARGET_DATA_TAG, Tag.TAG_COMPOUND.toInt())) {
                tag.getCompound(IO_TARGET_DATA_TAG).copy()
            } else {
                CompoundTag()
            }
        refreshIoCache()
        ioController.normalize()
    }

    private fun refreshIoCache() {
        cachedIoRoute =
            runCatching { IoRoute.valueOf(ioRouteName) }
                .getOrDefault(IoRoute.PASSIVE)
        cachedIoProviderId =
            ioProviderName
                .takeIf(String::isNotBlank)
                ?.let { runCatching { ResourceLocation.parse(it) }.getOrNull() }
        cachedIoTarget = cachedIoProviderId?.let { NetworkTargetRef(it, ioTargetData) }
    }

    companion object {
        private const val IO_ROUTE_FIELD = "ioRouteName"
        private const val IO_PROVIDER_FIELD = "ioProviderName"
        private const val IO_NETWORK_PAUSED_FIELD = "ioNetworkPaused"
        private const val IO_TARGET_DATA_TAG = "lazyIoTarget"
        private const val MANAGED_DATA_KEY = "managed"
    }
}

internal class IoRouteController(
    private val blockEntity: IoManagedBlockEntity,
) {
    private var adapter: IoRouteAdapter? = null
    private var retryTicks = 0

    val route: IoRoute
        get() = blockEntity.storedIoRoute()

    val networkPaused: Boolean
        get() = blockEntity.storedIoNetworkPaused()

    val target: NetworkTargetRef?
        get() = blockEntity.storedIoTarget()?.copy()

    val supportedRoutes: Set<IoRoute>
        get() = adapter?.supportedRoutes ?: setOf(IoRoute.PASSIVE)

    val resourceKinds: Set<IoResourceKind>
        get() = adapter?.resourceKinds ?: emptySet()

    internal fun install(newAdapter: IoRouteAdapter) {
        adapter = newAdapter
        normalize()
    }

    internal fun normalize() {
        val current = route
        if (!blockEntity.hasCanonicalStoredIoRoute()) {
            blockEntity.updateIoRoute(current)
        }
        if (current !in supportedRoutes) {
            setPassive()
            return
        }
        if (current != IoRoute.NETWORK) {
            blockEntity.updateIoProvider(null)
            blockEntity.updateIoNetworkPaused(false)
            return
        }
        val target = blockEntity.storedIoTarget()
        if (target == null || !currentAdapter().supportsNetworkTarget(target)) {
            setPassive()
        }
    }

    fun setRoute(newRoute: IoRoute): Boolean {
        if (newRoute !in supportedRoutes || newRoute == IoRoute.NETWORK) return false
        blockEntity.updateIoRoute(newRoute)
        blockEntity.updateIoProvider(null)
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
        return true
    }

    fun setNetworkTarget(newTarget: NetworkTargetRef): Boolean {
        if (IoRoute.NETWORK !in supportedRoutes) return false
        if (!currentAdapter().supportsNetworkTarget(newTarget)) return false
        blockEntity.updateIoRoute(IoRoute.NETWORK)
        blockEntity.updateIoProvider(newTarget)
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
        return true
    }

    fun setPassive() {
        blockEntity.updateIoRoute(IoRoute.PASSIVE)
        blockEntity.updateIoProvider(null)
        blockEntity.updateIoNetworkPaused(false)
        retryTicks = 0
    }

    fun tick() {
        val currentAdapter = adapter ?: return
        val currentRoute = route
        if (currentRoute !in currentAdapter.supportedRoutes) return
        if (currentRoute == IoRoute.PASSIVE && !currentAdapter.ticksWhenPassive) return
        if (retryTicks > 0) {
            retryTicks--
            return
        }

        val currentTarget = if (currentRoute == IoRoute.NETWORK) blockEntity.storedIoTarget() else null
        if (currentRoute == IoRoute.NETWORK && (currentTarget == null || networkPaused)) return

        when (currentAdapter.push(currentRoute, currentTarget)) {
            IoPushResult.Success -> retryTicks = 0
            IoPushResult.Retry -> retryTicks = NETWORK_RETRY_INTERVAL - 1
            IoPushResult.TargetMissing -> setPassive()
            IoPushResult.OutcomeUnknown -> {
                blockEntity.updateIoNetworkPaused(true)
                retryTicks = 0
            }
        }
    }

    private fun currentAdapter(): IoRouteAdapter = checkNotNull(adapter) { "IO route adapter has not been installed" }

    private companion object {
        const val NETWORK_RETRY_INTERVAL = 20
    }
}
