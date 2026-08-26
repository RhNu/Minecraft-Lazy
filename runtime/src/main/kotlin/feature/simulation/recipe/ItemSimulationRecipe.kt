package rhx.lazy.feature.simulation

import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.level.Level
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public data class ItemSimulationRecipe(
    val input: Ingredient,
    val duration: Int = USE_CONFIG_DEFAULT,
    val priority: Int = 0,
    val itemOutputs: List<SimulationItemOutput> = emptyList(),
    val fluidOutputs: List<SimulationFluidOutput> = emptyList(),
    val tools: List<SimulationToolRequirement> = emptyList(),
    val blockLootOutputs: List<SimulationBlockLootOutput> = emptyList(),
    val group: ResourceLocation = SimulationRecipeGroups.ITEM,
) : Recipe<SingleRecipeInput> {
    init {
        require(duration >= USE_CONFIG_DEFAULT) { "Simulation duration must be positive when specified" }
        requireValidSimulationTools(tools)
        require(itemOutputs.isNotEmpty() || fluidOutputs.isNotEmpty() || blockLootOutputs.isNotEmpty()) {
            "Simulation recipe must have at least one output"
        }
        require(effectiveOutputCount(itemOutputs, fluidOutputs, blockLootOutputs) <= MAX_OUTPUT_ENTRIES) {
            "Simulation recipe may declare at most $MAX_OUTPUT_ENTRIES outputs"
        }
    }

    fun durationTicks(): Int = if (duration == USE_CONFIG_DEFAULT) SimulationConfigs.settings.defaultDuration.get() else duration

    override fun matches(
        input: SingleRecipeInput,
        level: Level,
    ): Boolean = this.input.test(input.item())

    override fun assemble(
        input: SingleRecipeInput,
        registries: HolderLookup.Provider,
    ): ItemStack = ItemStack.EMPTY

    override fun canCraftInDimensions(
        width: Int,
        height: Int,
    ): Boolean = true

    override fun getResultItem(registries: HolderLookup.Provider): ItemStack = itemOutputs.firstOrNull()?.stack?.copy() ?: ItemStack.EMPTY

    override fun getIngredients(): NonNullList<Ingredient> = NonNullList.of(input)

    override fun isSpecial(): Boolean = true

    override fun getSerializer(): RecipeSerializer<*> = SimulationRegistries.itemRecipeSerializer.get()

    override fun getType(): RecipeType<*> = SimulationRegistries.itemRecipeType.get()

    public class Serializer : RecipeSerializer<ItemSimulationRecipe> {
        override fun codec(): MapCodec<ItemSimulationRecipe> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ItemSimulationRecipe> = STREAM_CODEC
    }

    companion object {
        const val DEFAULT_DURATION = 1200
        const val USE_CONFIG_DEFAULT = 0
        val POSITIVE_DURATION_CODEC: com.mojang.serialization.Codec<Int> =
            com.mojang.serialization.Codec.INT.validate { value ->
                if (value > 0) DataResult.success(value) else DataResult.error { "Simulation duration must be positive" }
            }

        val CODEC: MapCodec<ItemSimulationRecipe> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ItemSimulationRecipe::input),
                        POSITIVE_DURATION_CODEC
                            .optionalFieldOf("duration", USE_CONFIG_DEFAULT)
                            .forGetter(ItemSimulationRecipe::duration),
                        com.mojang.serialization.Codec.INT
                            .optionalFieldOf("priority", 0)
                            .forGetter(ItemSimulationRecipe::priority),
                        SimulationToolRequirement.CODEC
                            .listOf()
                            .optionalFieldOf("tools", emptyList())
                            .forGetter(ItemSimulationRecipe::tools),
                        SimulationItemOutput.CODEC
                            .codec()
                            .listOf()
                            .optionalFieldOf("item_outputs", emptyList())
                            .forGetter(ItemSimulationRecipe::itemOutputs),
                        SimulationFluidOutput.CODEC
                            .codec()
                            .listOf()
                            .optionalFieldOf("fluid_outputs", emptyList())
                            .forGetter(ItemSimulationRecipe::fluidOutputs),
                        SimulationBlockLootOutput.CODEC
                            .codec()
                            .listOf()
                            .optionalFieldOf("block_loot_outputs", emptyList())
                            .forGetter(ItemSimulationRecipe::blockLootOutputs),
                        ResourceLocation.CODEC
                            .optionalFieldOf("group", SimulationRecipeGroups.ITEM)
                            .forGetter(ItemSimulationRecipe::group),
                    ).apply(instance) { input, duration, priority, tools, itemOutputs, fluidOutputs, blockLootOutputs, group ->
                        ItemSimulationRecipe(input, duration, priority, itemOutputs, fluidOutputs, tools, blockLootOutputs, group)
                    }
            }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ItemSimulationRecipe> =
            StreamCodec.of(
                { buffer, recipe ->
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input)
                    buffer.writeVarInt(recipe.duration)
                    buffer.writeInt(recipe.priority)
                    encodeList(buffer, recipe.tools, SimulationToolRequirement::encode)
                    encodeList(buffer, recipe.itemOutputs, SimulationItemOutput::encode)
                    encodeList(buffer, recipe.fluidOutputs, SimulationFluidOutput::encode)
                    encodeList(buffer, recipe.blockLootOutputs, SimulationBlockLootOutput::encode)
                    ResourceLocation.STREAM_CODEC.encode(buffer, recipe.group)
                },
                { buffer ->
                    val input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                    val duration = buffer.readVarInt()
                    val priority = buffer.readInt()
                    val tools = decodeList(buffer, SimulationToolRequirement::decode)
                    val itemOutputs = decodeList(buffer, SimulationItemOutput::decode)
                    val fluidOutputs = decodeList(buffer, SimulationFluidOutput::decode)
                    val blockLootOutputs = decodeList(buffer, SimulationBlockLootOutput::decode)
                    val group = ResourceLocation.STREAM_CODEC.decode(buffer)
                    ItemSimulationRecipe(input, duration, priority, itemOutputs, fluidOutputs, tools, blockLootOutputs, group)
                },
            )
    }
}

private fun <T> encodeList(
    buffer: RegistryFriendlyByteBuf,
    values: List<T>,
    encode: (T, RegistryFriendlyByteBuf) -> Unit,
) {
    buffer.writeVarInt(values.size)
    values.forEach { value -> encode(value, buffer) }
}

private fun <T> decodeList(
    buffer: RegistryFriendlyByteBuf,
    decode: (RegistryFriendlyByteBuf) -> T,
): List<T> = List(buffer.readVarInt()) { decode(buffer) }
