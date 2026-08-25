package rhx.lazy.integration.jade

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceAmountStreamCodec
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import rhx.lazy.feature.repairer.RepairerBlockEntity
import rhx.lazy.feature.replicator.ReplicatorBlockEntity
import rhx.lazy.feature.simulation.SimulationChamberBlockEntity

internal data class BufferJadeData(
    val itemCount: Int,
    val fluidAmount: Int,
    override val output: JadeOutputState,
) : IoMachineJadeData

internal object BufferJadeDataProvider :
    IoMachineJadeDataProvider<BufferBlockEntity, BufferJadeData>(
        BufferBlockEntity::class.java,
        JadeProviderIds.buffer,
        BufferJadeDataCodec,
    ) {
    override fun createData(
        entity: BufferBlockEntity,
        output: JadeOutputState,
    ): BufferJadeData =
        BufferJadeData(
            itemCount = entity.totalItemCount,
            fluidAmount = entity.totalFluidAmount,
            output = output,
        )
}

private object BufferJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, BufferJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: BufferJadeData,
    ) {
        buffer.writeVarInt(value.itemCount)
        buffer.writeVarInt(value.fluidAmount)
        JadeOutputStateCodec.encode(buffer, value.output)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): BufferJadeData =
        BufferJadeData(
            itemCount = buffer.readVarInt(),
            fluidAmount = buffer.readVarInt(),
            output = JadeOutputStateCodec.decode(buffer),
        )
}

internal object EnergySourceJadeDataProvider :
    IoMachineJadeDataProvider<EnergySourceBlockEntity, OutputOnlyJadeData>(
        EnergySourceBlockEntity::class.java,
        JadeProviderIds.energySource,
        OutputOnlyJadeDataCodec,
    ) {
    override fun createData(
        entity: EnergySourceBlockEntity,
        output: JadeOutputState,
    ): OutputOnlyJadeData = OutputOnlyJadeData(output)
}

internal data class ReplicatorJadeData(
    val resource: ResourceAmount<out ResourceVariant>?,
    val intervalTicks: Int,
    override val output: JadeOutputState,
) : IoMachineJadeData

internal object ReplicatorJadeDataProvider :
    IoMachineJadeDataProvider<ReplicatorBlockEntity, ReplicatorJadeData>(
        ReplicatorBlockEntity::class.java,
        JadeProviderIds.replicator,
        ReplicatorJadeDataCodec,
    ) {
    override fun createData(
        entity: ReplicatorBlockEntity,
        output: JadeOutputState,
    ): ReplicatorJadeData =
        ReplicatorJadeData(
            resource = entity.getResource(),
            intervalTicks = entity.getGear().intervalTicks,
            output = output,
        )
}

private object ReplicatorJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, ReplicatorJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: ReplicatorJadeData,
    ) {
        buffer.writeBoolean(value.resource != null)
        value.resource?.let { ResourceAmountStreamCodec.encode(buffer, it) }
        buffer.writeVarInt(value.intervalTicks)
        JadeOutputStateCodec.encode(buffer, value.output)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): ReplicatorJadeData =
        ReplicatorJadeData(
            resource = if (buffer.readBoolean()) ResourceAmountStreamCodec.decode(buffer) else null,
            intervalTicks = buffer.readVarInt(),
            output = JadeOutputStateCodec.decode(buffer),
        )
}

internal object RepairerJadeDataProvider :
    MachineJadeDataProvider<RepairerBlockEntity, ItemStack>(
        RepairerBlockEntity::class.java,
        JadeProviderIds.repairer,
        ItemStack.OPTIONAL_STREAM_CODEC,
    ) {
    override fun createData(entity: RepairerBlockEntity): ItemStack = entity.itemHandler.getStackInSlot(0)
}

internal data class SimulationChamberJadeData(
    val progress: Float,
    val speed: Int,
    val outputMultiplier: Long,
    val pending: Boolean,
    override val output: JadeOutputState,
) : IoMachineJadeData

internal object SimulationChamberJadeDataProvider :
    IoMachineJadeDataProvider<SimulationChamberBlockEntity, SimulationChamberJadeData>(
        SimulationChamberBlockEntity::class.java,
        JadeProviderIds.simulationChamber,
        SimulationChamberJadeDataCodec,
    ) {
    override fun createData(
        entity: SimulationChamberBlockEntity,
        output: JadeOutputState,
    ): SimulationChamberJadeData =
        SimulationChamberJadeData(
            entity.progress(),
            entity.speedMultiplier(),
            entity.outputMultiplier(),
            entity.hasWaitingOutputs(),
            output,
        )
}

private object SimulationChamberJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, SimulationChamberJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: SimulationChamberJadeData,
    ) {
        buffer.writeFloat(value.progress)
        buffer.writeVarInt(value.speed)
        buffer.writeVarLong(value.outputMultiplier)
        buffer.writeBoolean(value.pending)
        JadeOutputStateCodec.encode(buffer, value.output)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): SimulationChamberJadeData =
        SimulationChamberJadeData(
            buffer.readFloat(),
            buffer.readVarInt(),
            buffer.readVarLong(),
            buffer.readBoolean(),
            JadeOutputStateCodec.decode(buffer),
        )
}
