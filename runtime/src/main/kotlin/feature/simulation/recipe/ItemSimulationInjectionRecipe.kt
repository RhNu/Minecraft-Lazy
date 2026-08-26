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

internal data class ItemSimulationInjectionRecipe(
    val input: Ingredient,
    val itemOutputs: List<SimulationItemOutput> = emptyList(),
    val fluidOutputs: List<SimulationFluidOutput> = emptyList(),
    val tools: List<SimulationToolRequirement> = emptyList(),
    val blockLootOutputs: List<SimulationBlockLootOutput> = emptyList(),
    val group: ResourceLocation = SimulationRecipeGroups.INJECTION,
) : Recipe<SingleRecipeInput> {
    init {
        requireValidSimulationTools(tools)
    }

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

    override fun getSerializer(): RecipeSerializer<*> = SimulationRegistries.itemInjectionRecipeSerializer.get()

    override fun getType(): RecipeType<*> = SimulationRegistries.itemInjectionRecipeType.get()

    public class Serializer : RecipeSerializer<ItemSimulationInjectionRecipe> {
        override fun codec(): MapCodec<ItemSimulationInjectionRecipe> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ItemSimulationInjectionRecipe> = STREAM_CODEC
    }

    companion object {
        val CODEC: MapCodec<ItemSimulationInjectionRecipe> =
            RecordCodecBuilder
                .mapCodec { instance ->
                    instance
                        .group(
                            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ItemSimulationInjectionRecipe::input),
                            SimulationToolRequirement.CODEC
                                .listOf()
                                .optionalFieldOf("tools", emptyList())
                                .forGetter(ItemSimulationInjectionRecipe::tools),
                            SimulationItemOutput.CODEC
                                .codec()
                                .listOf()
                                .optionalFieldOf("item_outputs", emptyList())
                                .forGetter(ItemSimulationInjectionRecipe::itemOutputs),
                            SimulationFluidOutput.CODEC
                                .codec()
                                .listOf()
                                .optionalFieldOf("fluid_outputs", emptyList())
                                .forGetter(ItemSimulationInjectionRecipe::fluidOutputs),
                            SimulationBlockLootOutput.CODEC
                                .codec()
                                .listOf()
                                .optionalFieldOf("block_loot_outputs", emptyList())
                                .forGetter(ItemSimulationInjectionRecipe::blockLootOutputs),
                            ResourceLocation.CODEC
                                .optionalFieldOf("group", SimulationRecipeGroups.INJECTION)
                                .forGetter(ItemSimulationInjectionRecipe::group),
                        ).apply(instance) { input, tools, itemOutputs, fluidOutputs, blockLootOutputs, group ->
                            ItemSimulationInjectionRecipe(input, itemOutputs, fluidOutputs, tools, blockLootOutputs, group)
                        }
                }.validate(::validate)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ItemSimulationInjectionRecipe> =
            StreamCodec.of(
                { buffer, recipe ->
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input)
                    encodeInjectionList(buffer, recipe.tools, SimulationToolRequirement::encode)
                    encodeInjectionList(buffer, recipe.itemOutputs, SimulationItemOutput::encode)
                    encodeInjectionList(buffer, recipe.fluidOutputs, SimulationFluidOutput::encode)
                    encodeInjectionList(buffer, recipe.blockLootOutputs, SimulationBlockLootOutput::encode)
                    ResourceLocation.STREAM_CODEC.encode(buffer, recipe.group)
                },
                { buffer ->
                    val input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                    val tools = decodeInjectionList(buffer, SimulationToolRequirement::decode)
                    val itemOutputs = decodeInjectionList(buffer, SimulationItemOutput::decode)
                    val fluidOutputs = decodeInjectionList(buffer, SimulationFluidOutput::decode)
                    val blockLootOutputs = decodeInjectionList(buffer, SimulationBlockLootOutput::decode)
                    val group = ResourceLocation.STREAM_CODEC.decode(buffer)
                    ItemSimulationInjectionRecipe(input, itemOutputs, fluidOutputs, tools, blockLootOutputs, group)
                        .also { require(validate(it).isSuccess) { validate(it).error().orElseThrow().message() } }
                },
            )

        private fun validate(recipe: ItemSimulationInjectionRecipe): DataResult<ItemSimulationInjectionRecipe> =
            when {
                recipe.itemOutputs.isEmpty() && recipe.fluidOutputs.isEmpty() && recipe.blockLootOutputs.isEmpty() ->
                    DataResult.error { "Simulation injection must have at least one output" }
                effectiveOutputCount(recipe.itemOutputs, recipe.fluidOutputs, recipe.blockLootOutputs) > MAX_OUTPUT_ENTRIES ->
                    DataResult.error { "Simulation injection may declare at most $MAX_OUTPUT_ENTRIES outputs" }
                else -> DataResult.success(recipe)
            }
    }
}

private fun <T> encodeInjectionList(
    buffer: RegistryFriendlyByteBuf,
    values: List<T>,
    encode: (T, RegistryFriendlyByteBuf) -> Unit,
) {
    buffer.writeVarInt(values.size)
    values.forEach { value -> encode(value, buffer) }
}

private fun <T> decodeInjectionList(
    buffer: RegistryFriendlyByteBuf,
    decode: (RegistryFriendlyByteBuf) -> T,
): List<T> = List(buffer.readVarInt()) { decode(buffer) }
