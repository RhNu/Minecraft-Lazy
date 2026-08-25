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
            buffer.writeVarInt(simulation.itemOutputs.size)
            simulation.itemOutputs.forEach { it.encode(buffer) }
            buffer.writeVarInt(simulation.fluidOutputs.size)
            simulation.fluidOutputs.forEach { it.encode(buffer) }
            buffer.writeVarInt(simulation.blockLootOutputs.size)
            simulation.blockLootOutputs.forEach { it.encode(buffer) }
        }

        private fun decode(buffer: RegistryFriendlyByteBuf): ResolvedSimulation.Item =
            ResolvedSimulation.Item(
                ResourceLocation.STREAM_CODEC.decode(buffer),
                buffer.readVarInt(),
                List(buffer.readVarInt()) { SimulationItemOutput.decode(buffer) },
                List(buffer.readVarInt()) { SimulationFluidOutput.decode(buffer) },
                List(buffer.readVarInt()) { SimulationBlockLootOutput.decode(buffer) },
            )
    }
}

@LazyInternalApi
public object AutomaticSimulationClientSnapshot {
    private var displays: List<AutomaticSimulationDisplay> = emptyList()
    private val listeners = linkedSetOf<(List<AutomaticSimulationDisplay>) -> Unit>()

    fun replace(newDisplays: List<AutomaticSimulationDisplay>) {
        displays = newDisplays
        listeners.forEach { it(newDisplays) }
    }

    fun all(): List<AutomaticSimulationDisplay> = displays

    fun find(stack: ItemStack): ResolvedSimulation.Item? = displays.firstOrNull { it.input.item === stack.item }?.simulation

    fun addListener(listener: (List<AutomaticSimulationDisplay>) -> Unit) {
        listeners += listener
    }
}
