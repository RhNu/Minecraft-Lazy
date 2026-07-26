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
import rhx.lazy.core.ManagedBlockEntity
import java.util.EnumMap

internal class ItemCopierBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : ManagedBlockEntity(ItemCopierRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private var template = ItemStack.EMPTY

    @field:Persisted
    @field:LazyManaged
    private var pushIntervalTicks = ItemCopierGear.DEFAULT.intervalTicks

    private var ticksUntilPush = 0

    private val neighborItemCaches =
        EnumMap<Direction, BlockCapabilityCache<IItemHandler, Direction?>>(Direction::class.java)

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
        if (template.isEmpty || !advanceSchedule()) return
        val serverLevel = level as? ServerLevel ?: return
        ItemCopierPusher.pushToHandlers(
            template,
            Direction.entries.map { direction ->
                cacheFor(serverLevel, direction).getCapability()
            },
        )
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

    companion object {
        internal const val MANAGED_DATA_KEY = "managed"
        internal const val TEMPLATE_FIELD = "template"
        internal const val PUSH_INTERVAL_FIELD = "pushIntervalTicks"
    }
}
