package rhx.lazy.feature.itemcopier

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.io.InfiniteOutputSource
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.ResourceKinds
import rhx.lazy.core.resource.itemAmount

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
    private val outputSource =
        InfiniteOutputSource {
            itemAmount(template, template.maxStackSize.coerceAtLeast(1).toLong())?.let(::listOf) ?: emptyList()
        }

    init {
        installIoAdapter(ItemCopierIoAdapter())
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

    override fun hasStoredContents(): Boolean = hasTemplate()

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
        requestImmediatePush()
    }

    private fun requestImmediatePush() {
        ticksUntilPush = 0
    }

    private inner class ItemCopierIoAdapter : IoAdapter {
        override val capabilities = setOf(ResourceKinds.ITEM)
        override val acceptsInput = false
        override val outputSource = this@ItemCopierBlockEntity.outputSource

        override fun readyToPush(): Boolean = !template.isEmpty && advanceSchedule()
    }

    companion object {
        internal const val TEMPLATE_FIELD = "template"
        internal const val PUSH_INTERVAL_FIELD = "pushIntervalTicks"
    }
}
