package rhx.lazy.feature.itemcopier

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoConfiguration
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoMode
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputRouter
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTransferResult
import java.util.EnumMap

internal class ItemCopierBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(ItemCopierRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private var template = ItemStack.EMPTY

    @field:Persisted
    @field:LazyManaged
    private var pushIntervalTicks = ItemCopierGear.DEFAULT.intervalTicks

    private var ticksUntilPush = 0

    private val neighborItemCaches =
        EnumMap<Direction, BlockCapabilityCache<IItemHandler, Direction?>>(Direction::class.java)

    private val ioAdapter = ItemCopierIoAdapter()

    init {
        installIoAdapter(ioAdapter)
    }

    fun getTemplate(): ItemStack = if (template.isEmpty) ItemStack.EMPTY else template.copy()

    fun getGear(): ItemCopierGear = ItemCopierGear.fromInterval(pushIntervalTicks)

    fun setTemplate(stack: ItemStack) {
        val normalized = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        if (!ItemStack.matches(template, normalized)) {
            template = normalized
            markDirty(TEMPLATE_FIELD)
        }
        requestImmediatePush()
    }

    fun clearTemplate() {
        setTemplate(ItemStack.EMPTY)
    }

    fun cycleGear() {
        setGear(getGear().next())
    }

    fun setGear(gear: ItemCopierGear) {
        if (pushIntervalTicks != gear.intervalTicks) {
            pushIntervalTicks = gear.intervalTicks
            markDirty(PUSH_INTERVAL_FIELD)
        }
        requestImmediatePush()
    }

    fun hasTemplate(): Boolean = !template.isEmpty

    fun onServerTick() {
        if (template.isEmpty) return
        ioController.tick()
    }

    internal fun advanceSchedule(): Boolean {
        if (ticksUntilPush > 0) {
            ticksUntilPush--
            return false
        }
        ticksUntilPush = getGear().intervalTicks - 1
        return true
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        template = if (template.isEmpty) ItemStack.EMPTY else template.copyWithCount(1)
        pushIntervalTicks = getGear().intervalTicks
        neighborItemCaches.clear()
        requestImmediatePush()
    }

    override fun setRemoved() {
        neighborItemCaches.clear()
        super.setRemoved()
    }

    private fun requestImmediatePush() {
        ticksUntilPush = 0
    }

    private fun cacheFor(
        level: ServerLevel,
        direction: Direction,
    ): BlockCapabilityCache<IItemHandler, Direction?> =
        neighborItemCaches.getOrPut(direction) {
            BlockCapabilityCache.create<IItemHandler, Direction?>(
                Capabilities.ItemHandler.BLOCK,
                level,
                blockPos.relative(direction),
                direction.opposite,
                { !isRemoved },
                {},
            )
        }

    private inner class ItemCopierIoAdapter : IoAdapter {
        override val capabilities = setOf(NetworkInsertCapabilities.ITEM)

        override fun push(configuration: IoConfiguration): IoPushResult {
            if (configuration.mode == IoMode.PASSIVE) return IoPushResult.Success
            if (configuration.mode == IoMode.FACE && ioController.outputDirections().isEmpty()) return IoPushResult.Success
            if (!advanceSchedule()) return IoPushResult.Success
            return when (configuration.mode) {
                IoMode.FACE -> {
                    val serverLevel = level as? ServerLevel ?: return IoPushResult.Retry
                    ItemCopierPusher.pushToHandlers(
                        template,
                        ioController.outputDirections().map { direction -> cacheFor(serverLevel, direction).getCapability() },
                    )
                    IoPushResult.Success
                }

                IoMode.NETWORK -> pushToNetwork(configuration.networkTarget)
                IoMode.PASSIVE -> IoPushResult.Success
            }
        }

        private fun pushToNetwork(target: NetworkTargetRef?): IoPushResult {
            val networkTarget = target ?: return IoPushResult.TargetMissing
            val result =
                NetworkOutputRouter.insert(
                    networkTarget,
                    NetworkPayload.Items(template.copyWithCount(1), template.maxStackSize.toLong()),
                    false,
                )
            return when (result) {
                is NetworkTransferResult.Success -> IoPushResult.Success
                NetworkTransferResult.TargetMissing -> IoPushResult.TargetMissing
                NetworkTransferResult.InvalidTarget -> IoPushResult.TargetMissing
                NetworkTransferResult.OutcomeUnknown -> IoPushResult.OutcomeUnknown
                NetworkTransferResult.TemporarilyUnavailable -> IoPushResult.Retry
            }
        }
    }

    companion object {
        internal const val MANAGED_DATA_KEY = "managed"
        internal const val TEMPLATE_FIELD = "template"
        internal const val PUSH_INTERVAL_FIELD = "pushIntervalTicks"
    }
}
