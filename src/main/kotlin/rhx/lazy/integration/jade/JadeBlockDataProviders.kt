package rhx.lazy.integration.jade

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.feature.energy.EnergyOutputMode
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import rhx.lazy.feature.itemcopier.ItemCopierBlockEntity
import rhx.lazy.feature.repairer.RepairerBlockEntity
import snownee.jade.api.BlockAccessor
import snownee.jade.api.StreamServerDataProvider

internal data class BufferJadeData(
    val itemCount: Int,
    val fluidAmount: Int,
    val networkForwarding: Boolean,
)

internal object BufferJadeDataProvider : StreamServerDataProvider<BlockAccessor, BufferJadeData> {
    override fun streamData(accessor: BlockAccessor): BufferJadeData? {
        val blockEntity = accessor.blockEntity as? BufferBlockEntity ?: return null
        return BufferJadeData(
            itemCount = blockEntity.totalItemCount,
            fluidAmount = blockEntity.totalFluidAmount,
            networkForwarding = blockEntity.isNetworkForwardingEnabled,
        )
    }

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, BufferJadeData> = BufferJadeDataCodec

    override fun getUid(): ResourceLocation = JadeProviderIds.buffer
}

private object BufferJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, BufferJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: BufferJadeData,
    ) {
        buffer.writeVarInt(value.itemCount)
        buffer.writeVarInt(value.fluidAmount)
        buffer.writeBoolean(value.networkForwarding)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): BufferJadeData =
        BufferJadeData(
            itemCount = buffer.readVarInt(),
            fluidAmount = buffer.readVarInt(),
            networkForwarding = buffer.readBoolean(),
        )
}

internal object EnergySourceJadeDataProvider : StreamServerDataProvider<BlockAccessor, EnergyOutputMode> {
    override fun streamData(accessor: BlockAccessor): EnergyOutputMode? = (accessor.blockEntity as? EnergySourceBlockEntity)?.outputMode()

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, EnergyOutputMode> = EnergyOutputModeCodec

    override fun getUid(): ResourceLocation = JadeProviderIds.energySource
}

private object EnergyOutputModeCodec : StreamCodec<RegistryFriendlyByteBuf, EnergyOutputMode> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: EnergyOutputMode,
    ) {
        buffer.writeVarInt(value.ordinal)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): EnergyOutputMode =
        EnergyOutputMode.entries.getOrElse(buffer.readVarInt()) { EnergyOutputMode.PASSIVE }
}

internal data class ItemCopierJadeData(
    val template: ItemStack,
    val intervalTicks: Int,
)

internal object ItemCopierJadeDataProvider : StreamServerDataProvider<BlockAccessor, ItemCopierJadeData> {
    override fun streamData(accessor: BlockAccessor): ItemCopierJadeData? {
        val blockEntity = accessor.blockEntity as? ItemCopierBlockEntity ?: return null
        return ItemCopierJadeData(
            template = blockEntity.getTemplate(),
            intervalTicks = blockEntity.getGear().intervalTicks,
        )
    }

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ItemCopierJadeData> = ItemCopierJadeDataCodec

    override fun getUid(): ResourceLocation = JadeProviderIds.itemCopier
}

private object ItemCopierJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, ItemCopierJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: ItemCopierJadeData,
    ) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.template)
        buffer.writeVarInt(value.intervalTicks)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): ItemCopierJadeData =
        ItemCopierJadeData(
            template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
            intervalTicks = buffer.readVarInt(),
        )
}

internal object RepairerJadeDataProvider : StreamServerDataProvider<BlockAccessor, ItemStack> {
    override fun streamData(accessor: BlockAccessor): ItemStack? =
        (accessor.blockEntity as? RepairerBlockEntity)?.itemHandler?.getStackInSlot(0)

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ItemStack> = ItemStack.OPTIONAL_STREAM_CODEC

    override fun getUid(): ResourceLocation = JadeProviderIds.repairer
}
