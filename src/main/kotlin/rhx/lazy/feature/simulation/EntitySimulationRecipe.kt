package rhx.lazy.feature.simulation

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.neoforged.neoforge.fluids.FluidStack
import java.util.Optional

internal data class EntitySimulationRecipe(
    val entity: ResourceLocation,
    val duration: Int = ItemSimulationRecipe.USE_CONFIG_DEFAULT,
    val priority: Int = 0,
    val rollLootTable: Boolean = true,
    val lootTable: Optional<ResourceLocation> = Optional.empty(),
    val itemOutputs: List<SimulationItemOutput> = emptyList(),
    val fluidOutputs: List<SimulationFluidOutput> = emptyList(),
    val displayItemOutputs: List<ItemStack> = emptyList(),
    val displayFluidOutputs: List<FluidStack> = emptyList(),
) : Recipe<RecipeInput> {
    init {
        require(duration >= ItemSimulationRecipe.USE_CONFIG_DEFAULT) { "Simulation duration must be positive when specified" }
        require(rollLootTable || itemOutputs.isNotEmpty() || fluidOutputs.isNotEmpty()) {
            "Entity simulation must roll a loot table or declare an output"
        }
        require(itemOutputs.size + fluidOutputs.size + displayItemOutputs.size + displayFluidOutputs.size <= MAX_OUTPUT_ENTRIES) {
            "Entity simulation may declare at most $MAX_OUTPUT_ENTRIES combined runtime and display outputs"
        }
    }

    fun durationTicks(): Int =
        if (duration ==
            ItemSimulationRecipe.USE_CONFIG_DEFAULT
        ) {
            SimulationConfigs.settings.defaultDuration.get()
        } else {
            duration
        }

    override fun matches(
        input: RecipeInput,
        level: Level,
    ): Boolean = false

    override fun assemble(
        input: RecipeInput,
        registries: HolderLookup.Provider,
    ): ItemStack = ItemStack.EMPTY

    override fun canCraftInDimensions(
        width: Int,
        height: Int,
    ): Boolean = true

    override fun getResultItem(registries: HolderLookup.Provider): ItemStack =
        itemOutputs.firstOrNull()?.stack?.copy()
            ?: displayItemOutputs.firstOrNull()?.copy()
            ?: ItemStack.EMPTY

    override fun isSpecial(): Boolean = true

    override fun getSerializer(): RecipeSerializer<*> = SimulationRegistries.entityRecipeSerializer.get()

    override fun getType(): RecipeType<*> = SimulationRegistries.entityRecipeType.get()

    internal class Serializer : RecipeSerializer<EntitySimulationRecipe> {
        override fun codec(): MapCodec<EntitySimulationRecipe> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, EntitySimulationRecipe> = STREAM_CODEC
    }

    companion object {
        private val ENTITY_ID_CODEC: Codec<ResourceLocation> =
            ResourceLocation.CODEC.validate { id ->
                if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    com.mojang.serialization.DataResult
                        .success(id)
                } else {
                    com.mojang.serialization.DataResult
                        .error { "Unknown entity type $id" }
                }
            }
        val CODEC: MapCodec<EntitySimulationRecipe> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        ENTITY_ID_CODEC.fieldOf("entity").forGetter(EntitySimulationRecipe::entity),
                        ItemSimulationRecipe.POSITIVE_DURATION_CODEC
                            .optionalFieldOf("duration", ItemSimulationRecipe.USE_CONFIG_DEFAULT)
                            .forGetter(EntitySimulationRecipe::duration),
                        Codec.INT.optionalFieldOf("priority", 0).forGetter(EntitySimulationRecipe::priority),
                        Codec.BOOL.optionalFieldOf("roll_loot_table", true).forGetter(EntitySimulationRecipe::rollLootTable),
                        ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter(EntitySimulationRecipe::lootTable),
                        SimulationItemOutput.CODEC
                            .codec()
                            .listOf()
                            .optionalFieldOf("item_outputs", emptyList())
                            .forGetter(EntitySimulationRecipe::itemOutputs),
                        SimulationFluidOutput.CODEC
                            .codec()
                            .listOf()
                            .optionalFieldOf("fluid_outputs", emptyList())
                            .forGetter(EntitySimulationRecipe::fluidOutputs),
                        ItemStack.CODEC
                            .listOf()
                            .optionalFieldOf("display_item_outputs", emptyList())
                            .forGetter(EntitySimulationRecipe::displayItemOutputs),
                        FluidStack.CODEC
                            .listOf()
                            .optionalFieldOf("display_fluid_outputs", emptyList())
                            .forGetter(EntitySimulationRecipe::displayFluidOutputs),
                    ).apply(instance, ::EntitySimulationRecipe)
            }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, EntitySimulationRecipe> =
            StreamCodec.of(
                { buffer, recipe ->
                    ResourceLocation.STREAM_CODEC.encode(buffer, recipe.entity)
                    buffer.writeVarInt(recipe.duration)
                    buffer.writeInt(recipe.priority)
                    buffer.writeBoolean(recipe.rollLootTable)
                    buffer.writeBoolean(recipe.lootTable.isPresent)
                    recipe.lootTable.ifPresent { ResourceLocation.STREAM_CODEC.encode(buffer, it) }
                    encodeList(buffer, recipe.itemOutputs, SimulationItemOutput::encode)
                    encodeList(buffer, recipe.fluidOutputs, SimulationFluidOutput::encode)
                    encodeList(buffer, recipe.displayItemOutputs) { value, target -> ItemStack.STREAM_CODEC.encode(target, value) }
                    encodeList(buffer, recipe.displayFluidOutputs) { value, target -> FluidStack.STREAM_CODEC.encode(target, value) }
                },
                { buffer ->
                    EntitySimulationRecipe(
                        ResourceLocation.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        buffer.readInt(),
                        buffer.readBoolean(),
                        if (buffer.readBoolean()) {
                            Optional.of(ResourceLocation.STREAM_CODEC.decode(buffer))
                        } else {
                            Optional.empty()
                        },
                        decodeList(buffer, SimulationItemOutput::decode),
                        decodeList(buffer, SimulationFluidOutput::decode),
                        decodeList(buffer) { ItemStack.STREAM_CODEC.decode(it) },
                        decodeList(buffer) { FluidStack.STREAM_CODEC.decode(it) },
                    )
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
