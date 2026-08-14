package rhx.lazy.feature.simulation

import com.mojang.serialization.Codec
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import java.util.Optional

internal sealed interface SimulationBatch {
    val remaining: Long

    fun withRemaining(remaining: Long): SimulationBatch

    fun save(registries: HolderLookup.Provider): CompoundTag {
        val context = registries.createSerializationContext(NbtOps.INSTANCE)
        return CompoundTag().apply {
            putString(KIND_TAG, kind)
            putLong(REMAINING_TAG, remaining)
            when (val batch = this@SimulationBatch) {
                is Item -> {
                    put(ITEM_OUTPUTS_TAG, encodeList(context, SimulationItemOutput.CODEC.codec(), batch.itemOutputs))
                    put(FLUID_OUTPUTS_TAG, encodeList(context, SimulationFluidOutput.CODEC.codec(), batch.fluidOutputs))
                }
                is Automatic -> put(AUTOMATIC_OUTPUT_TAG, batch.output.save(registries))
                is Entity -> {
                    putString(ENTITY_TAG, batch.entityId.toString())
                    putBoolean(ROLL_LOOT_TAG, batch.rollLootTable)
                    batch.lootTable.ifPresent { putString(LOOT_TABLE_TAG, it.toString()) }
                    put(ITEM_OUTPUTS_TAG, encodeList(context, SimulationItemOutput.CODEC.codec(), batch.itemOutputs))
                    put(FLUID_OUTPUTS_TAG, encodeList(context, SimulationFluidOutput.CODEC.codec(), batch.fluidOutputs))
                }
            }
        }
    }

    data class Item(
        val itemOutputs: List<SimulationItemOutput>,
        val fluidOutputs: List<SimulationFluidOutput>,
        override val remaining: Long,
    ) : SimulationBatch {
        init {
            require(remaining > 0L)
            require(itemOutputs.size + fluidOutputs.size <= MAX_OUTPUT_ENTRIES)
        }

        override fun withRemaining(remaining: Long): SimulationBatch = copy(remaining = remaining)
    }

    data class Automatic(
        val output: ItemStack,
        override val remaining: Long,
    ) : SimulationBatch {
        init {
            require(!output.isEmpty)
            require(remaining > 0L)
        }

        override fun withRemaining(remaining: Long): SimulationBatch = copy(remaining = remaining)
    }

    data class Entity(
        val entityId: ResourceLocation,
        val rollLootTable: Boolean,
        val lootTable: Optional<ResourceLocation>,
        val itemOutputs: List<SimulationItemOutput>,
        val fluidOutputs: List<SimulationFluidOutput>,
        override val remaining: Long,
    ) : SimulationBatch {
        init {
            require(remaining > 0L)
            require(itemOutputs.size + fluidOutputs.size <= MAX_OUTPUT_ENTRIES)
        }

        override fun withRemaining(remaining: Long): SimulationBatch = copy(remaining = remaining)
    }

    private val kind: String
        get() =
            when (this) {
                is Item -> ITEM_KIND
                is Automatic -> AUTOMATIC_KIND
                is Entity -> ENTITY_KIND
            }

    companion object {
        fun from(
            simulation: ResolvedSimulation,
            rolls: Long,
        ): SimulationBatch =
            when (simulation) {
                is ResolvedSimulation.ItemRecipe ->
                    Item(
                        simulation.holder
                            .value()
                            .itemOutputs
                            .map(::copy),
                        simulation.holder
                            .value()
                            .fluidOutputs
                            .map(::copy),
                        rolls,
                    )
                is ResolvedSimulation.AutomaticMineral -> Automatic(simulation.output.copy(), rolls)
                is ResolvedSimulation.EntityProfile -> {
                    val recipe = simulation.holder?.value()
                    Entity(
                        simulation.entityId,
                        recipe?.rollLootTable ?: true,
                        recipe?.lootTable ?: Optional.empty(),
                        recipe?.itemOutputs?.map(::copy).orEmpty(),
                        recipe?.fluidOutputs?.map(::copy).orEmpty(),
                        rolls,
                    )
                }
            }

        fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): SimulationBatch? {
            val remaining = tag.getLong(REMAINING_TAG)
            if (remaining <= 0L) return null
            val context = registries.createSerializationContext(NbtOps.INSTANCE)
            return runCatching {
                when (tag.getString(KIND_TAG)) {
                    ITEM_KIND ->
                        Item(
                            decodeList(
                                context,
                                SimulationItemOutput.CODEC.codec(),
                                tag.getList(ITEM_OUTPUTS_TAG, Tag.TAG_COMPOUND.toInt()),
                            ),
                            decodeList(
                                context,
                                SimulationFluidOutput.CODEC.codec(),
                                tag.getList(FLUID_OUTPUTS_TAG, Tag.TAG_COMPOUND.toInt()),
                            ),
                            remaining,
                        )
                    AUTOMATIC_KIND -> {
                        val stack = ItemStack.parseOptional(registries, tag.getCompound(AUTOMATIC_OUTPUT_TAG))
                        stack.takeUnless(ItemStack::isEmpty)?.let { Automatic(it, remaining) }
                    }
                    ENTITY_KIND -> {
                        val entityId = ResourceLocation.tryParse(tag.getString(ENTITY_TAG)) ?: return null
                        Entity(
                            entityId,
                            tag.getBoolean(ROLL_LOOT_TAG),
                            ResourceLocation.tryParse(tag.getString(LOOT_TABLE_TAG)).let(Optional<ResourceLocation>::ofNullable),
                            decodeList(
                                context,
                                SimulationItemOutput.CODEC.codec(),
                                tag.getList(ITEM_OUTPUTS_TAG, Tag.TAG_COMPOUND.toInt()),
                            ),
                            decodeList(
                                context,
                                SimulationFluidOutput.CODEC.codec(),
                                tag.getList(FLUID_OUTPUTS_TAG, Tag.TAG_COMPOUND.toInt()),
                            ),
                            remaining,
                        )
                    }
                    else -> null
                }
            }.getOrNull()
        }

        private fun copy(output: SimulationItemOutput) = output.copy(stack = output.stack.copy())

        private fun copy(output: SimulationFluidOutput) = output.copy(stack = output.stack.copy())

        private fun <T> encodeList(
            context: com.mojang.serialization.DynamicOps<Tag>,
            codec: Codec<T>,
            values: List<T>,
        ): ListTag =
            ListTag().apply {
                values.forEach { value -> add(codec.encodeStart(context, value).result().orElseThrow()) }
            }

        private fun <T> decodeList(
            context: com.mojang.serialization.DynamicOps<Tag>,
            codec: Codec<T>,
            values: ListTag,
        ): List<T> = values.map { codec.parse(context, it).result().orElseThrow() }

        private const val KIND_TAG = "kind"
        private const val REMAINING_TAG = "remaining"
        private const val ENTITY_TAG = "entity"
        private const val ROLL_LOOT_TAG = "rollLootTable"
        private const val LOOT_TABLE_TAG = "lootTable"
        private const val ITEM_OUTPUTS_TAG = "itemOutputs"
        private const val FLUID_OUTPUTS_TAG = "fluidOutputs"
        private const val AUTOMATIC_OUTPUT_TAG = "automaticOutput"
        private const val ITEM_KIND = "item"
        private const val AUTOMATIC_KIND = "automatic"
        private const val ENTITY_KIND = "entity"
    }
}
