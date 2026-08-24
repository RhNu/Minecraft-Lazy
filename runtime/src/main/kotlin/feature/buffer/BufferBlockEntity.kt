package rhx.lazy.feature.buffer

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.ResourceKinds
import rhx.lazy.core.io.StoredOutputSource
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ResourceFluidHandler
import rhx.lazy.core.resource.ResourceItemHandler
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class BufferBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(BufferRegistries.blockEntity.get(), pos, state) {
    private val items = ResourceStore(ItemResourceKind, ITEM_SLOT_COUNT, ITEM_SLOT_CAPACITY.toLong(), ::itemsChanged)
    private val fluids = ResourceStore(FluidResourceKind, FLUID_TANK_COUNT, FLUID_TANK_CAPACITY.toLong(), ::fluidsChanged)
    private val outputSource = StoredOutputSource(listOf(items, fluids))

    private var itemTotal = 0

    private var fluidTotal = 0

    val itemHandler: IItemHandlerModifiable = ResourceItemHandler(items)
    val fluidHandler: IFluidHandler = ResourceFluidHandler(fluids)

    init {
        installIoAdapter(BufferIoAdapter())
    }

    val totalItemCount: Int
        get() = itemTotal

    val totalFluidAmount: Int
        get() = fluidTotal

    fun hasContents(): Boolean = !items.isEmpty || !fluids.isEmpty

    override fun hasStoredContents(): Boolean = hasContents()

    fun clearContents(): Boolean {
        if (!hasContents()) return false
        items.clear()
        fluids.clear()
        return true
    }

    fun getItemTemplate(slot: Int): ItemStack = items.variant(slot)?.template ?: ItemStack.EMPTY

    fun getItemCount(slot: Int): Int = items.amount(slot).toInt()

    fun getFluid(slot: Int): FluidStack = fluids.variant(slot)?.template?.copyWithAmount(fluids.amount(slot).toInt()) ?: FluidStack.EMPTY

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        if (!items.isEmpty) tag.put(ITEM_STORE_TAG, items.save(registries))
        if (!fluids.isEmpty) tag.put(FLUID_STORE_TAG, fluids.save(registries))
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        items.load(registries, tag.getList(ITEM_STORE_TAG, Tag.TAG_COMPOUND.toInt()))
        fluids.load(registries, tag.getList(FLUID_STORE_TAG, Tag.TAG_COMPOUND.toInt()))
        refreshTotals()
    }

    fun onServerTick() {
        ioController.tick()
    }

    private fun itemsChanged() {
        refreshTotals()
        setChanged()
    }

    private fun fluidsChanged() {
        refreshTotals()
        setChanged()
    }

    private fun refreshTotals() {
        itemTotal = items.snapshot().sumOf { it.amount }.toInt()
        fluidTotal = fluids.snapshot().sumOf { it.amount }.toInt()
    }

    private inner class BufferIoAdapter : IoAdapter {
        override val capabilities = setOf(ResourceKinds.ITEM, ResourceKinds.FLUID)
        override val outputSource = this@BufferBlockEntity.outputSource
    }

    companion object {
        const val ITEM_SLOT_COUNT = 8
        const val ITEM_SLOT_CAPACITY = 256
        const val FLUID_TANK_COUNT = 4
        const val FLUID_TANK_CAPACITY = 64_000
        const val TOTAL_ITEM_CAPACITY = ITEM_SLOT_COUNT * ITEM_SLOT_CAPACITY
        const val TOTAL_FLUID_CAPACITY = FLUID_TANK_COUNT * FLUID_TANK_CAPACITY

        public const val ITEM_STORE_TAG = "resourcesItems"
        public const val FLUID_STORE_TAG = "resourcesFluids"
    }
}
