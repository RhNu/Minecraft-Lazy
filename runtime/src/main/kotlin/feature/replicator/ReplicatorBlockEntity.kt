package rhx.lazy.feature.replicator

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.io.InfiniteOutputSource
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceKinds
import rhx.lazy.core.resource.ResourceSprite
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class ReplicatorBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(ReplicatorRegistries.blockEntity.get(), pos, state) {
    private var resource: ResourceAmount<out ResourceVariant>? = null

    @field:Persisted
    @field:LazyManaged
    private var pushIntervalTicks = ReplicatorGear.DEFAULT.intervalTicks

    private var ticksUntilPush = 0
    private val outputSource = InfiniteOutputSource { getResource()?.let(::listOf) ?: emptyList() }

    init {
        installIoAdapter(ReplicatorIoAdapter())
    }

    public fun getResource(): ResourceAmount<out ResourceVariant>? = resource?.let(::copyAmount)

    internal fun getResourceName() = resource?.variantName

    internal fun getResourceSprite(): ResourceSprite? = resource?.sprite

    public fun setResource(amount: ResourceAmount<out ResourceVariant>?) {
        val normalized = amount?.let(::copyAmount)
        if (!sameAmount(resource, normalized)) {
            resource = normalized
            setChanged()
        }
        requestImmediatePush()
    }

    public fun clearResource() {
        setResource(null)
    }

    public fun <V : ResourceVariant> markResource(
        kind: ResourceKind<V>,
        variant: V,
    ) {
        val current = resource
        val markedAmount =
            if (current != null && sameVariant(current, kind, variant)) {
                current.amount
            } else {
                kind.defaultAmount
            }
        setResource(ResourceAmount(kind, variant, markedAmount))
    }

    internal fun markResource(amount: ResourceAmount<out ResourceVariant>) {
        markResourceUnchecked(amount)
    }

    public fun setAmount(amount: Long) {
        val current = resource ?: return
        if (amount > 0L) {
            setResource(current.withAmount(amount))
        }
    }

    public fun adjustAmount(delta: Long) {
        val current = resource ?: return
        val adjusted =
            when {
                delta > 0L && current.amount > Long.MAX_VALUE - delta -> Long.MAX_VALUE
                delta < 0L && current.amount < 1L - delta -> 1L
                else -> current.amount + delta
            }
        setAmount(adjusted)
    }

    public fun amountStep(): Long = resource?.kind?.defaultAmount ?: 1L

    internal fun getItemTemplate(): ItemStack {
        val amount = resource ?: return ItemStack.EMPTY
        val variant = amount.variant as? ItemVariant ?: return ItemStack.EMPTY
        return variant.template.copyWithCount(1)
    }

    internal fun setItemTemplate(stack: ItemStack) {
        if (stack.isEmpty) {
            if (resource?.variant is ItemVariant) clearResource()
        } else {
            ItemVariant.of(stack)?.let { markResource(ResourceKinds.ITEM, it) }
        }
    }

    internal fun getFluidTemplate(): FluidStack {
        val amount = resource ?: return FluidStack.EMPTY
        val variant = amount.variant as? FluidVariant ?: return FluidStack.EMPTY
        return variant.template.copyWithAmount(ResourceKinds.FLUID.defaultAmount.toInt())
    }

    internal fun setFluidTemplate(stack: FluidStack) {
        if (stack.isEmpty) {
            if (resource?.variant is FluidVariant) clearResource()
        } else {
            FluidVariant.of(stack)?.let { markResource(ResourceKinds.FLUID, it) }
        }
    }

    public fun getGear(): ReplicatorGear = ReplicatorGear.fromInterval(pushIntervalTicks)

    internal fun cycleGear() {
        setGear(getGear().next())
    }

    internal fun setGear(gear: ReplicatorGear) {
        if (pushIntervalTicks != gear.intervalTicks) {
            pushIntervalTicks = gear.intervalTicks
            markDirty(PUSH_INTERVAL_FIELD)
        }
        requestImmediatePush()
    }

    internal fun hasResource(): Boolean = resource != null

    override fun hasStoredContents(): Boolean = hasResource()

    internal fun onServerTick() {
        if (resource == null) return
        ioController.tick()
    }

    public fun advanceSchedule(): Boolean {
        if (ticksUntilPush > 0) {
            ticksUntilPush--
            return false
        }
        ticksUntilPush = getGear().intervalTicks - 1
        return true
    }

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        resource?.let { tag.put(RESOURCE_TAG, it.save(registries)) }
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        resource = ResourceAmount.parse(registries, tag.getCompound(RESOURCE_TAG))
        pushIntervalTicks = getGear().intervalTicks
        requestImmediatePush()
    }

    private fun requestImmediatePush() {
        ticksUntilPush = 0
    }

    private inner class ReplicatorIoAdapter : IoAdapter {
        override val acceptsInput = false
        override val outputSource = this@ReplicatorBlockEntity.outputSource

        override fun readyToPush(): Boolean = resource != null && advanceSchedule()
    }

    companion object {
        public const val RESOURCE_TAG = "resource"
        public const val PUSH_INTERVAL_FIELD = "pushIntervalTicks"

        @Suppress("UNCHECKED_CAST")
        private fun copyAmount(amount: ResourceAmount<out ResourceVariant>): ResourceAmount<out ResourceVariant> =
            (amount as ResourceAmount<ResourceVariant>).copyAmount()

        @Suppress("UNCHECKED_CAST")
        private fun <V : ResourceVariant> sameVariant(
            amount: ResourceAmount<out ResourceVariant>,
            kind: ResourceKind<V>,
            variant: V,
        ): Boolean {
            if (amount.kind !== kind) return false
            return (amount as ResourceAmount<V>).matches(kind, variant)
        }

        @Suppress("UNCHECKED_CAST")
        private fun sameAmount(
            first: ResourceAmount<out ResourceVariant>?,
            second: ResourceAmount<out ResourceVariant>?,
        ): Boolean {
            if (first === second) return true
            return first != null && second != null && first.amount == second.amount && first.matches(second)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun markResourceUnchecked(amount: ResourceAmount<out ResourceVariant>) {
        val typed = amount as ResourceAmount<ResourceVariant>
        markResource(typed.kind, typed.variant)
    }
}
