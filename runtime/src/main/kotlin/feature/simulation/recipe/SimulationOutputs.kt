package rhx.lazy.feature.simulation

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.integration.api.LazyInternalApi
import java.util.Optional

@LazyInternalApi
public const val MAX_OUTPUT_ENTRIES = 28

@LazyInternalApi
public data class SimulationItemOutput(
    val stack: ItemStack,
    val chance: Float = 1f,
    val minRolls: Int = 1,
    val maxRolls: Int = 1,
) {
    init {
        require(!stack.isEmpty) { "Simulation item output must not be empty" }
        validateOutputRange(chance, minRolls, maxRolls)
    }

    fun encode(buffer: RegistryFriendlyByteBuf) {
        ItemStack.STREAM_CODEC.encode(buffer, stack)
        buffer.writeFloat(chance)
        buffer.writeVarInt(minRolls)
        buffer.writeVarInt(maxRolls)
    }

    companion object {
        val CODEC: MapCodec<SimulationItemOutput> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        ItemStack.CODEC.fieldOf("stack").forGetter(SimulationItemOutput::stack),
                        CHANCE_CODEC.optionalFieldOf("chance", 1f).forGetter(SimulationItemOutput::chance),
                        NON_NEGATIVE_INT.optionalFieldOf("min_rolls", 1).forGetter(SimulationItemOutput::minRolls),
                        NON_NEGATIVE_INT.optionalFieldOf("max_rolls", 1).forGetter(SimulationItemOutput::maxRolls),
                    ).apply(instance, ::SimulationItemOutput)
            }

        fun decode(buffer: RegistryFriendlyByteBuf): SimulationItemOutput =
            SimulationItemOutput(
                ItemStack.STREAM_CODEC.decode(buffer),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
            )
    }
}

@LazyInternalApi
public data class SimulationFluidOutput(
    val stack: FluidStack,
    val chance: Float = 1f,
    val minRolls: Int = 1,
    val maxRolls: Int = 1,
) {
    init {
        require(!stack.isEmpty) { "Simulation fluid output must not be empty" }
        validateOutputRange(chance, minRolls, maxRolls)
    }

    fun encode(buffer: RegistryFriendlyByteBuf) {
        FluidStack.STREAM_CODEC.encode(buffer, stack)
        buffer.writeFloat(chance)
        buffer.writeVarInt(minRolls)
        buffer.writeVarInt(maxRolls)
    }

    companion object {
        val CODEC: MapCodec<SimulationFluidOutput> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        FluidStack.CODEC.fieldOf("stack").forGetter(SimulationFluidOutput::stack),
                        CHANCE_CODEC.optionalFieldOf("chance", 1f).forGetter(SimulationFluidOutput::chance),
                        NON_NEGATIVE_INT.optionalFieldOf("min_rolls", 1).forGetter(SimulationFluidOutput::minRolls),
                        NON_NEGATIVE_INT.optionalFieldOf("max_rolls", 1).forGetter(SimulationFluidOutput::maxRolls),
                    ).apply(instance, ::SimulationFluidOutput)
            }

        fun decode(buffer: RegistryFriendlyByteBuf): SimulationFluidOutput =
            SimulationFluidOutput(
                FluidStack.STREAM_CODEC.decode(buffer),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
            )
    }
}

@LazyInternalApi
public data class SimulationBlockLootOutput(
    val state: BlockState,
    val displayItems: List<ItemStack> = emptyList(),
    val tool: ItemStack = ItemStack.EMPTY,
    val chance: Float = 1f,
    val minRolls: Int = 1,
    val maxRolls: Int = 1,
) {
    init {
        validateOutputRange(chance, minRolls, maxRolls)
    }

    fun encode(buffer: RegistryFriendlyByteBuf) {
        STATE_STREAM_CODEC.encode(buffer, state)
        buffer.writeVarInt(displayItems.size)
        displayItems.forEach { ItemStack.STREAM_CODEC.encode(buffer, it) }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, tool)
        buffer.writeFloat(chance)
        buffer.writeVarInt(minRolls)
        buffer.writeVarInt(maxRolls)
    }

    companion object {
        val CODEC: MapCodec<SimulationBlockLootOutput> =
            RecordCodecBuilder.mapCodec { instance ->
                instance
                    .group(
                        BlockState.CODEC.fieldOf("state").forGetter(SimulationBlockLootOutput::state),
                        ItemStack.CODEC
                            .listOf()
                            .optionalFieldOf("display_items", emptyList())
                            .forGetter(SimulationBlockLootOutput::displayItems),
                        ItemStack.CODEC.optionalFieldOf("tool").forGetter { output ->
                            Optional.of(output.tool).filter { !it.isEmpty }
                        },
                        CHANCE_CODEC.optionalFieldOf("chance", 1f).forGetter(SimulationBlockLootOutput::chance),
                        NON_NEGATIVE_INT.optionalFieldOf("min_rolls", 1).forGetter(SimulationBlockLootOutput::minRolls),
                        NON_NEGATIVE_INT.optionalFieldOf("max_rolls", 1).forGetter(SimulationBlockLootOutput::maxRolls),
                    ).apply(instance) { state, displayItems, tool, chance, minRolls, maxRolls ->
                        SimulationBlockLootOutput(state, displayItems, tool.orElse(ItemStack.EMPTY), chance, minRolls, maxRolls)
                    }
            }

        fun decode(buffer: RegistryFriendlyByteBuf): SimulationBlockLootOutput =
            SimulationBlockLootOutput(
                STATE_STREAM_CODEC.decode(buffer),
                List(buffer.readVarInt()) { ItemStack.STREAM_CODEC.decode(buffer) },
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
            )

        private val STATE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BlockState> =
            ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC)
    }
}

private val CHANCE_CODEC: Codec<Float> =
    Codec.FLOAT.validate { value ->
        if (value in 0f..1f) DataResult.success(value) else DataResult.error { "Chance must be in [0, 1]" }
    }

private val NON_NEGATIVE_INT: Codec<Int> =
    Codec.INT.validate { value ->
        if (value >= 0) DataResult.success(value) else DataResult.error { "Roll count must be non-negative" }
    }

private fun validateOutputRange(
    chance: Float,
    minRolls: Int,
    maxRolls: Int,
) {
    require(chance in 0f..1f) { "Chance must be in [0, 1]" }
    require(minRolls >= 0) { "Minimum rolls must be non-negative" }
    require(maxRolls >= minRolls) { "Maximum rolls must be greater than or equal to minimum rolls" }
}
