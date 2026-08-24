package rhx.lazy.feature.shaping

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.io.IoAdapter
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.ResourceKinds
import rhx.lazy.core.io.StoredOutputSource
import rhx.lazy.core.material.MaterialIndex
import rhx.lazy.core.material.MaterialIndexes
import rhx.lazy.core.render.MachineActivity
import rhx.lazy.core.render.MachineDisplayState
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceDelta
import rhx.lazy.core.resource.ResourceItemHandler
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.core.resource.ResourceTransaction
import rhx.lazy.core.resource.StoreDelta
import rhx.lazy.integration.api.LazyInternalApi

/**
 * Converts every input resource identity into one chosen material form in the tick it arrives.
 *
 * There is no progress, no recipe lookup and no batch state: an entry is an item, the index turns that
 * item into a material and a unit value, and [shaperTrade] turns two unit values into whole-item
 * exchanges. What an entry cannot fill a whole trade with simply stays put.
 *
 * The eight entries are a bound on resource diversity and per-tick work, not eight independent
 * machines. Quantities are long-count values and all exchanges commit atomically.
 */
@LazyInternalApi
public class ShaperBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : IoManagedBlockEntity(ShaperRegistries.blockEntity.get(), pos, state) {
    /** The chosen form, stored as the item a player showed the machine. Never consumed. */
    private var sample = ItemStack.EMPTY

    private var blocked = false
    private var lastConversionTick = Long.MIN_VALUE
    private var workCursor = 0
    private val inputs = ResourceStore(ItemResourceKind, ENTRIES, ENTRY_CAPACITY, ::resourcesChanged)
    private val outputs = ResourceStore(ItemResourceKind, ENTRIES, ENTRY_CAPACITY, ::resourcesChanged)
    private val outputSource = StoredOutputSource(listOf(outputs))

    val inputHandler = ResourceItemHandler(inputs, allowInsert = true, isValid = ::isValidInput)
    val outputHandler = ResourceItemHandler(outputs, allowInsert = false)

    init {
        installIoAdapter(ShaperIoAdapter())
    }

    fun serverTick() {
        val serverLevel = level as? ServerLevel ?: return
        val ioCycle = ioController.beginTick()
        convert(serverLevel)
        ioController.endTick(ioCycle)
        tickDisplayState()
    }

    fun sampleStack(): ItemStack = if (sample.isEmpty) ItemStack.EMPTY else sample.copy()

    fun hasSample(): Boolean = !sample.isEmpty

    fun hasInvalidSample(): Boolean = !sample.isEmpty && MaterialIndexes.current().formOf(sample.item) == null

    fun isBlocked(): Boolean = blocked

    fun inputAmount(entry: Int): Long = inputs.amount(entry)

    fun outputAmount(entry: Int): Long = outputs.amount(entry)

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
        if (!inputs.isEmpty) tag.put(INPUT_STORE_TAG, inputs.save(registries))
        if (!outputs.isEmpty) tag.put(OUTPUT_STORE_TAG, outputs.save(registries))
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
        inputs.load(registries, tag.getList(INPUT_STORE_TAG, Tag.TAG_COMPOUND.toInt()))
        outputs.load(registries, tag.getList(OUTPUT_STORE_TAG, Tag.TAG_COMPOUND.toInt()))
        blocked = false
        workCursor = 0
        lastConversionTick = Long.MIN_VALUE
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
     * One pass over the entries. Every step is a map lookup or integer arithmetic, so a machine with a
     * full output pool costs the same as an empty one: the trade count comes out zero and the entry is
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
        var visited = 0
        while (visited < ENTRIES) {
            val entry = (workCursor + visited) % ENTRIES
            visited++
            val template = inputs.variant(entry)?.template ?: continue
            val available = inputs.amount(entry)
            if (available <= 0L) continue
            val source = index.formOf(template.item) ?: continue
            if (source.form == match.form) continue
            val product = index.itemFor(source.material, match.form) ?: continue
            if (ItemStack(product).`is`(ShaperTags.outputBlacklist)) continue
            val trade = shaperTrade(source.units, targetUnits) ?: continue
            val productVariant = requireNotNull(ItemVariant.of(ItemStack(product)))
            val trades = trade.trades(available, outputs.capacityFor(productVariant))
            if (trades <= 0L) {
                if (available >= trade.inputPerTrade) blockedNow = true
                continue
            }
            val consumed =
                ResourceAmount(ItemResourceKind, requireNotNull(inputs.variant(entry)), Math.multiplyExact(trades, trade.inputPerTrade))
            val produced = ResourceAmount(ItemResourceKind, productVariant, Math.multiplyExact(trades, trade.outputPerTrade))
            if (
                ResourceTransaction.tryApply(
                    StoreDelta(inputs, ResourceDelta(extracted = listOf(consumed))),
                    StoreDelta(outputs, ResourceDelta(inserted = listOf(produced))),
                )
            ) {
                converted = true
            } else {
                blockedNow = true
            }
        }
        workCursor = (workCursor + 1) % ENTRIES

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
        for (entry in 0 until ENTRIES) {
            val template = inputs.variant(entry)?.template ?: continue
            if (inputs.amount(entry) <= 0L) continue
            index.formOf(template.item)?.let { return it.material }
        }
        return index.formOf(sample.item)?.material
    }

    private fun isValidInput(stack: ItemStack): Boolean {
        if (stack.`is`(ShaperTags.inputBlacklist)) return false
        return MaterialIndexes.current().formOf(stack.item) != null
    }

    private inner class ShaperIoAdapter : IoAdapter {
        override val capabilities = setOf(ResourceKinds.ITEM)
        override val outputSource = this@ShaperBlockEntity.outputSource
    }

    private fun resourcesChanged() {
        setChanged()
    }

    companion object {
        const val ENTRIES = 8
        const val CAPABILITY_SLOT_LIMIT = Int.MAX_VALUE
        private const val ENTRY_CAPACITY = Long.MAX_VALUE

        /** How long a conversion keeps the machine lit, so the display poll cannot miss a busy tick. */
        private const val ACTIVITY_WINDOW = 20L

        private const val INPUT_STORE_TAG = "resourcesIn"
        private const val OUTPUT_STORE_TAG = "resourcesOut"
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
