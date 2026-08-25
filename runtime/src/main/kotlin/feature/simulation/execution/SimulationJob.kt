package rhx.lazy.feature.simulation

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

/** Everything that can affect an in-flight simulation, captured when the progress cycle starts. */
internal class SimulationJob(
    target: ItemStack,
    var batch: SimulationBatch,
    val duration: Int,
    val speedMultiplier: Int,
    val outputMultiplier: Long,
    tools: List<ItemStack>,
    var progressTicks: Int = 0,
) {
    val target: ItemStack = target.copyWithCount(1)
    val tools: List<ItemStack> = tools.map { if (it.isEmpty) ItemStack.EMPTY else it.copyWithCount(1) }

    init {
        require(duration > 0)
        require(speedMultiplier > 0)
        require(outputMultiplier > 0L)
    }

    fun save(registries: HolderLookup.Provider): CompoundTag =
        CompoundTag().apply {
            put(TARGET_TAG, target.save(registries))
            put(BATCH_TAG, batch.save(registries))
            putInt(DURATION_TAG, duration)
            putInt(SPEED_TAG, speedMultiplier)
            putLong(OUTPUT_MULTIPLIER_TAG, outputMultiplier)
            putInt(PROGRESS_TAG, progressTicks.coerceIn(0, duration))
            put(
                TOOLS_TAG,
                ListTag().apply {
                    tools.forEachIndexed { slot, stack ->
                        if (!stack.isEmpty) {
                            add(
                                CompoundTag().apply {
                                    putInt(SLOT_TAG, slot)
                                    put(STACK_TAG, stack.save(registries))
                                },
                            )
                        }
                    }
                },
            )
        }

    companion object {
        fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): SimulationJob? {
            val target = ItemStack.parseOptional(registries, tag.getCompound(TARGET_TAG))
            val batch = SimulationBatch.parse(registries, tag.getCompound(BATCH_TAG)) ?: return null
            val duration = tag.getInt(DURATION_TAG)
            val speed = tag.getInt(SPEED_TAG)
            val outputMultiplier = tag.getLong(OUTPUT_MULTIPLIER_TAG)
            if (target.isEmpty || duration <= 0 || speed <= 0 || outputMultiplier <= 0L) return null
            val tools = MutableList(SimulationChamberBlockEntity.TOOL_SLOTS) { ItemStack.EMPTY }
            tag.getList(TOOLS_TAG, Tag.TAG_COMPOUND.toInt()).forEach { raw ->
                val entry = raw as? CompoundTag ?: return@forEach
                val slot = entry.getInt(SLOT_TAG)
                if (slot in tools.indices) tools[slot] = ItemStack.parseOptional(registries, entry.getCompound(STACK_TAG))
            }
            return SimulationJob(
                target,
                batch,
                duration,
                speed,
                outputMultiplier,
                tools,
                tag.getInt(PROGRESS_TAG).coerceIn(0, duration),
            )
        }

        private const val TARGET_TAG = "target"
        private const val BATCH_TAG = "batch"
        private const val DURATION_TAG = "duration"
        private const val SPEED_TAG = "speed"
        private const val OUTPUT_MULTIPLIER_TAG = "outputMultiplier"
        private const val PROGRESS_TAG = "progress"
        private const val TOOLS_TAG = "tools"
        private const val SLOT_TAG = "slot"
        private const val STACK_TAG = "stack"
    }
}
