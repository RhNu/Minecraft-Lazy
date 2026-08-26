package rhx.lazy.feature.simulation

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public const val MAX_SIMULATION_TOOLS: Int = 3

/** A non-consuming condition matched against one of the simulation chamber's tool slots. */
@LazyInternalApi
public sealed interface SimulationToolRequirement {
    public fun matches(stack: ItemStack): Boolean

    @LazyInternalApi
    public data class Item(
        val ingredient: Ingredient,
    ) : SimulationToolRequirement {
        override fun matches(stack: ItemStack): Boolean = ingredient.test(stack)
    }

    @LazyInternalApi
    public data class BlockTag(
        val tag: TagKey<net.minecraft.world.level.block.Block>,
    ) : SimulationToolRequirement {
        override fun matches(stack: ItemStack): Boolean = (stack.item as? BlockItem)?.block?.defaultBlockState()?.`is`(tag) == true
    }

    public companion object {
        private enum class Kind(
            val id: String,
        ) {
            ITEM("item"),
            BLOCK_TAG("block_tag"),
        }

        private val ITEM_CODEC: MapCodec<Item> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Item::ingredient))
                    .apply(instance, ::Item)
            }
        private val BLOCK_TAG_CODEC: MapCodec<BlockTag> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(BlockTag::tag))
                    .apply(instance, ::BlockTag)
            }

        public val CODEC: Codec<SimulationToolRequirement> =
            Codec.STRING
                .flatXmap(
                    { id ->
                        Kind.entries
                            .firstOrNull { it.id == id }
                            ?.let(DataResult<Kind>::success)
                            ?: DataResult.error { "Unknown simulation tool requirement type $id" }
                    },
                    { kind -> DataResult.success(kind.id) },
                ).dispatch(
                    "type",
                    { requirement -> if (requirement is Item) Kind.ITEM else Kind.BLOCK_TAG },
                    { kind -> if (kind == Kind.ITEM) ITEM_CODEC else BLOCK_TAG_CODEC },
                )

        public fun encode(
            requirement: SimulationToolRequirement,
            buffer: RegistryFriendlyByteBuf,
        ) {
            when (requirement) {
                is Item -> {
                    buffer.writeByte(0)
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, requirement.ingredient)
                }
                is BlockTag -> {
                    buffer.writeByte(1)
                    ResourceLocation.STREAM_CODEC.encode(buffer, requirement.tag.location)
                }
            }
        }

        public fun decode(buffer: RegistryFriendlyByteBuf): SimulationToolRequirement =
            when (val kind = buffer.readUnsignedByte().toInt()) {
                0 -> Item(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
                1 -> BlockTag(TagKey.create(Registries.BLOCK, ResourceLocation.STREAM_CODEC.decode(buffer)))
                else -> error("Unknown simulation tool requirement network type $kind")
            }
    }
}

/**
 * Matches requirements to distinct non-empty slots. Requirements and slots are both unordered;
 * additional stacks are deliberately ignored so behavior tools may coexist with recipe tools.
 */
@LazyInternalApi
public fun simulationToolsMatch(
    requirements: List<SimulationToolRequirement>,
    tools: List<ItemStack>,
): Boolean {
    if (requirements.size > MAX_SIMULATION_TOOLS) return false
    val candidates = tools.take(MAX_SIMULATION_TOOLS)
    if (requirements.isEmpty()) return true
    val used = BooleanArray(candidates.size)

    fun match(requirementIndex: Int): Boolean {
        if (requirementIndex == requirements.size) return true
        candidates.indices.forEach { toolIndex ->
            if (!used[toolIndex] && requirements[requirementIndex].matches(candidates[toolIndex])) {
                used[toolIndex] = true
                if (match(requirementIndex + 1)) return true
                used[toolIndex] = false
            }
        }
        return false
    }

    return match(0)
}

@LazyInternalApi
public fun simulationToolDisplayStacks(requirement: SimulationToolRequirement): List<ItemStack> =
    when (requirement) {
        is SimulationToolRequirement.Item -> requirement.ingredient.items.map(ItemStack::copy)
        is SimulationToolRequirement.BlockTag ->
            BuiltInRegistries.BLOCK
                .getTag(requirement.tag)
                .orElse(null)
                ?.mapNotNull { holder ->
                    holder
                        .value()
                        .asItem()
                        .takeUnless { it === Items.AIR }
                        ?.let(::ItemStack)
                }?.toList()
                .orEmpty()
    }

internal fun requireValidSimulationTools(tools: List<SimulationToolRequirement>) {
    require(tools.size <= MAX_SIMULATION_TOOLS) { "Simulation recipe may require at most $MAX_SIMULATION_TOOLS tools" }
}
