package rhx.lazy.feature.shaping

import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.IItemHandlerModifiable
import net.neoforged.neoforge.items.ItemHandlerHelper
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoPushResult
import rhx.lazy.core.io.NeighborCapabilities
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOffer
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.offer
import rhx.lazy.core.material.MaterialIndex
import rhx.lazy.core.material.MaterialIndexes
import rhx.lazy.core.render.MachineActivity
import rhx.lazy.core.render.MachineDisplayState

/**
 * Converts every input lane into one chosen material form, in the tick the material arrives.
 *
 * There is no progress, no recipe lookup and no batch state: a lane is an item, the index turns that
 * item into a material and a unit value, and [shaperTrade] turns two unit values into whole-item
 * exchanges. What a lane cannot fill a whole trade with simply stays put.
 *
 * The eight lanes are the performance ceiling, not just the capacity. One machine can hold at most
 * eight materials, so per-tick work is bounded at eight map lookups no matter how much throughput
 * runs through it — which is exactly why a lane holds 1024 pieces instead of 64.
 */
internal class ShaperBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(ShaperRegistries.blockEntity.get(), pos, state) {
    @field:Persisted
    @field:LazyManaged
    private val inputTemplates = MutableList(LANES) { ItemStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private val inputCounts = MutableList(LANES) { 0 }

    @field:Persisted
    @field:LazyManaged
    private val outputTemplates = MutableList(LANES) { ItemStack.EMPTY }

    @field:Persisted
    @field:LazyManaged
    private val outputCounts = MutableList(LANES) { 0 }

    /** The chosen form, stored as the item a player showed the machine. Never consumed. */
    private var sample = ItemStack.EMPTY

    private var blocked = false
    private var lastConversionTick = Long.MIN_VALUE
    private val neighborItems = NeighborCapabilities.items(blockPos) { !isRemoved }

    private val inputs =
        ShaperLanes(inputTemplates, inputCounts, LANES, LANE_CAPACITY) {
            markDirty(INPUT_TEMPLATES_FIELD)
            markDirty(INPUT_COUNTS_FIELD)
            setChanged()
        }

    private val outputs =
        ShaperLanes(outputTemplates, outputCounts, LANES, LANE_CAPACITY) {
            markDirty(OUTPUT_TEMPLATES_FIELD)
            markDirty(OUTPUT_COUNTS_FIELD)
            setChanged()
        }

    val inputHandler: IItemHandlerModifiable = ShaperLaneHandler(inputs, allowInsert = true, ::isValidInput)
    val outputHandler: IItemHandlerModifiable = ShaperLaneHandler(outputs, allowInsert = false) { false }

    init {
        installIoAdapter(ShaperIoAdapter())
    }

    fun serverTick() {
        val serverLevel = level as? ServerLevel ?: return
        convert(serverLevel)
        ioController.tick()
        tickDisplayState()
    }

    fun sampleStack(): ItemStack = if (sample.isEmpty) ItemStack.EMPTY else sample.copy()

    fun hasSample(): Boolean = !sample.isEmpty

    fun hasInvalidSample(): Boolean = !sample.isEmpty && MaterialIndexes.current().formOf(sample.item) == null

    fun isBlocked(): Boolean = blocked

    /**
     * Records the form to convert to. Rejects anything with no known form, so a slot that refuses an
     * item is telling the player that item is not part of any material family the pack declares.
     */
    fun setSample(stack: ItemStack): Boolean {
        val normalized = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        if (!normalized.isEmpty && MaterialIndexes.current().formOf(normalized.item) == null) return false
        if (ItemStack.matches(sample, normalized)) return true
        sample = normalized
        setChanged()
        refreshDisplayState()
        return true
    }

    override fun hasStoredContents(): Boolean = !inputs.isEmpty || !outputs.isEmpty

    override fun settingKeys(): Set<String> = super.settingKeys() + SAMPLE_FIELD

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)
        if (!sample.isEmpty) tag.put(SAMPLE_FIELD, sample.save(registries))
    }

    override fun computeDisplayState(): MachineDisplayState {
        if (sample.isEmpty) return MachineDisplayState.EMPTY
        return MachineDisplayState(sample.copy(), activity())
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        sample =
            ItemStack.parseOptional(registries, tag.getCompound(SAMPLE_FIELD)).let {
                if (it.isEmpty) ItemStack.EMPTY else it.copyWithCount(1)
            }
        inputs.normalize()
        outputs.normalize()
        blocked = false
        lastConversionTick = Long.MIN_VALUE
        neighborItems.invalidate()
    }

    override fun setRemoved() {
        neighborItems.invalidate()
        super.setRemoved()
    }

    /**
     * The full conversion table for whatever material the machine is currently working on, so the
     * sample slot answers "what do I feed it, and what comes out" without opening a recipe viewer.
     */
    fun conversionTooltip(): Component {
        if (sample.isEmpty) return Component.translatable("gui.lazy.shaper.sample.empty")
        val index = MaterialIndexes.current()
        val target = index.formOf(sample.item) ?: return Component.translatable("gui.lazy.shaper.sample.unknown")
        val targetUnits = index.unitsOf(target.form) ?: return Component.translatable("gui.lazy.shaper.sample.unknown")
        val material =
            summaryMaterial(index)
                ?: return Component.translatable("gui.lazy.shaper.sample.selected", sample.hoverName)
        val product =
            index.itemFor(material, target.form)
                ?: return Component.translatable("gui.lazy.shaper.sample.unavailable", sample.hoverName)

        val productName = ItemStack(product).hoverName
        val lines = mutableListOf(Component.translatable("gui.lazy.shaper.sample.selected", productName))
        index.forms.forEach forms@{ form ->
            if (form == target.form) return@forms
            val source = index.itemFor(material, form) ?: return@forms
            val units = index.unitsOf(form) ?: return@forms
            val trade = shaperTrade(units, targetUnits) ?: return@forms
            lines +=
                Component.translatable(
                    "gui.lazy.shaper.conversion",
                    trade.inputPerTrade,
                    ItemStack(source).hoverName,
                    trade.outputPerTrade,
                    productName,
                )
        }
        return joinLines(lines)
    }

    /**
     * One pass over the lanes. Every step is a map lookup or integer arithmetic, so a machine with a
     * full output pool costs the same as an empty one: the trade count comes out zero and the lane is
     * skipped without writing anything.
     */
    private fun convert(level: ServerLevel) {
        if (sample.isEmpty || inputs.isEmpty) {
            blocked = false
            return
        }
        val index = MaterialIndexes.current()
        val match = index.formOf(sample.item)
        val targetUnits = match?.let { index.unitsOf(it.form) }
        if (match == null || targetUnits == null) {
            blocked = false
            return
        }

        var blockedNow = false
        var converted = false
        for (lane in 0 until LANES) {
            val template = inputs.template(lane)
            val available = inputs.count(lane)
            if (template.isEmpty || available <= 0) continue
            val source = index.formOf(template.item) ?: continue
            if (source.form == match.form) continue
            val product = index.itemFor(source.material, match.form) ?: continue
            if (ItemStack(product).`is`(ShaperTags.outputBlacklist)) continue
            val trade = shaperTrade(source.units, targetUnits) ?: continue
            val productStack = ItemStack(product)
            val trades = trade.trades(available, outputs.capacityFor(productStack))
            if (trades <= 0) {
                if (available >= trade.inputPerTrade) blockedNow = true
                continue
            }
            inputs.take(lane, trades * trade.inputPerTrade)
            outputs.insert(productStack, trades * trade.outputPerTrade)
            converted = true
        }

        if (converted) lastConversionTick = level.gameTime
        blocked = blockedNow
    }

    private fun activity(): MachineActivity {
        val gameTime = level?.gameTime ?: return MachineActivity.IDLE
        return when {
            gameTime - lastConversionTick <= ACTIVITY_WINDOW -> MachineActivity.RUNNING
            blocked -> MachineActivity.BLOCKED
            else -> MachineActivity.IDLE
        }
    }

    /** What the tooltip describes: the loaded material, or the sample's own when nothing is loaded. */
    private fun summaryMaterial(index: MaterialIndex): String? {
        for (lane in 0 until LANES) {
            val template = inputs.template(lane)
            if (template.isEmpty || inputs.count(lane) <= 0) continue
            index.formOf(template.item)?.let { return it.material }
        }
        return index.formOf(sample.item)?.material
    }

    private fun isValidInput(stack: ItemStack): Boolean {
        if (stack.`is`(ShaperTags.inputBlacklist)) return false
        return MaterialIndexes.current().formOf(stack.item) != null
    }

    private inner class ShaperIoAdapter : IoAdapter {
        override val capabilities = setOf(NetworkInsertCapabilities.ITEM)

        override fun pushToFaces(directions: Set<Direction>): IoPushResult {
            val serverLevel = level as? ServerLevel ?: return IoPushResult.Retry
            directions.forEach { direction ->
                val target = neighborItems[serverLevel, direction] ?: return@forEach
                for (lane in 0 until LANES) {
                    val stack = outputs.stackInLane(lane)
                    if (stack.isEmpty) continue
                    val remainder = ItemHandlerHelper.insertItemStacked(target, stack, false)
                    val remaining = remainder.count.coerceIn(0, stack.count)
                    if (remaining != stack.count) outputs.take(lane, stack.count - remaining)
                }
            }
            return IoPushResult.Success
        }

        override fun pushToNetwork(target: NetworkTargetRef): IoPushResult {
            for (lane in 0 until LANES) {
                val stack = outputs.stackInLane(lane)
                if (stack.isEmpty) continue
                val amount = stack.count.toLong()
                when (val offer = target.offer(NetworkPayload.Items(stack.copyWithCount(1), amount), amount)) {
                    is NetworkOffer.Accepted -> if (offer.accepted > 0L) outputs.take(lane, offer.accepted.toInt())
                    is NetworkOffer.Rejected -> return offer.push
                }
            }
            return IoPushResult.Success
        }
    }

    companion object {
        const val LANES = 8
        const val LANE_CAPACITY = 1024

        /** How long a conversion keeps the machine lit, so the display poll cannot miss a busy tick. */
        private const val ACTIVITY_WINDOW = 20L

        private const val INPUT_TEMPLATES_FIELD = "inputTemplates"
        private const val INPUT_COUNTS_FIELD = "inputCounts"
        private const val OUTPUT_TEMPLATES_FIELD = "outputTemplates"
        private const val OUTPUT_COUNTS_FIELD = "outputCounts"
        private const val SAMPLE_FIELD = "sample"

        private fun joinLines(lines: List<Component>): Component =
            Component.empty().apply {
                lines.forEachIndexed { index, line ->
                    if (index > 0) append("\n")
                    append(line)
                }
            }
    }
}
