package rhx.lazy.feature.simulation

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.lazyId
import rhx.lazy.integration.api.LazyInternalApi

internal data class AutomaticSimulationSnapshotPayload(
    val displays: List<AutomaticSimulationDisplay>,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<AutomaticSimulationSnapshotPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<AutomaticSimulationSnapshotPayload>(lazyId("automatic_simulation_snapshot"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, AutomaticSimulationSnapshotPayload> =
            StreamCodec.of(
                { buffer, payload ->
                    buffer.writeVarInt(payload.displays.size)
                    payload.displays.forEach { display ->
                        ItemStack.STREAM_CODEC.encode(buffer, display.input)
                        encode(buffer, display.simulation)
                    }
                },
                { buffer ->
                    AutomaticSimulationSnapshotPayload(
                        List(buffer.readVarInt()) {
                            AutomaticSimulationDisplay(
                                ItemStack.STREAM_CODEC.decode(buffer),
                                decode(buffer),
                            )
                        },
                    )
                },
            )

        private fun encode(
            buffer: RegistryFriendlyByteBuf,
            simulation: ResolvedSimulation.Item,
        ) {
            buffer.writeResourceLocation(simulation.id)
            buffer.writeVarInt(simulation.duration)
            buffer.writeResourceLocation(simulation.group)
            buffer.writeInt(simulation.priority)
            buffer.writeVarInt(simulation.tools.size)
            simulation.tools.forEach { SimulationToolRequirement.encode(it, buffer) }
            buffer.writeVarInt(simulation.itemOutputs.size)
            simulation.itemOutputs.forEach { it.encode(buffer) }
            buffer.writeVarInt(simulation.fluidOutputs.size)
            simulation.fluidOutputs.forEach { it.encode(buffer) }
            buffer.writeVarInt(simulation.blockLootOutputs.size)
            simulation.blockLootOutputs.forEach { it.encode(buffer) }
        }

        private fun decode(buffer: RegistryFriendlyByteBuf): ResolvedSimulation.Item =
            run {
                val id = ResourceLocation.STREAM_CODEC.decode(buffer)
                val duration = buffer.readVarInt()
                val group = ResourceLocation.STREAM_CODEC.decode(buffer)
                val priority = buffer.readInt()
                val tools = List(buffer.readVarInt()) { SimulationToolRequirement.decode(buffer) }
                ResolvedSimulation.Item(
                    id,
                    duration,
                    List(buffer.readVarInt()) { SimulationItemOutput.decode(buffer) },
                    List(buffer.readVarInt()) { SimulationFluidOutput.decode(buffer) },
                    List(buffer.readVarInt()) { SimulationBlockLootOutput.decode(buffer) },
                    tools,
                    group,
                    priority,
                )
            }
    }
}

@LazyInternalApi
public object AutomaticSimulationClientSnapshot {
    private var displays: List<AutomaticSimulationDisplay> = emptyList()
    private val listeners = linkedSetOf<(List<AutomaticSimulationDisplay>) -> Unit>()

    fun replace(newDisplays: List<AutomaticSimulationDisplay>) {
        displays = newDisplays
        SimulationRecipeResolver.invalidateTargetCaches()
        listeners.forEach { it(newDisplays) }
    }

    fun all(): List<AutomaticSimulationDisplay> = displays

    fun find(
        stack: ItemStack,
        tools: List<ItemStack> = emptyList(),
    ): ResolvedSimulation.Item? =
        displays
            .asSequence()
            .filter { it.input.item === stack.item && simulationToolsMatch(it.simulation.tools, tools) }
            .sortedWith(compareByDescending<AutomaticSimulationDisplay> { it.simulation.tools.size }.thenBy { it.simulation.id.toString() })
            .firstOrNull()
            ?.simulation

    fun supports(stack: ItemStack): Boolean = displays.any { it.input.item === stack.item }

    fun addListener(listener: (List<AutomaticSimulationDisplay>) -> Unit) {
        listeners += listener
    }
}
