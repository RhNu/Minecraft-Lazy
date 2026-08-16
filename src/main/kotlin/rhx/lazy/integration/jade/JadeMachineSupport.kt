package rhx.lazy.integration.jade

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import rhx.lazy.core.io.IoManagedBlockEntity
import rhx.lazy.core.io.IoMode
import snownee.jade.api.BlockAccessor
import snownee.jade.api.StreamServerDataProvider

/** Data shared by every IO-managed machine's Jade view. */
internal interface IoMachineJadeData {
    val output: JadeOutputState
}

/**
 * Stable wire representation of the common machine output row.
 *
 * The network target is represented by its provider ID so the client can use the provider's
 * localized display name without sending already-localized text over the network.
 */
internal data class JadeOutputState(
    val mode: IoMode,
    val autoEject: Boolean,
    val networkProviderId: ResourceLocation?,
)

internal object JadeOutputStateCodec : StreamCodec<RegistryFriendlyByteBuf, JadeOutputState> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: JadeOutputState,
    ) {
        buffer.writeVarInt(value.mode.ordinal)
        buffer.writeBoolean(value.autoEject)
        buffer.writeBoolean(value.networkProviderId != null)
        value.networkProviderId?.let(buffer::writeResourceLocation)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): JadeOutputState =
        JadeOutputState(
            mode = IoMode.entries.getOrElse(buffer.readVarInt()) { IoMode.PASSIVE },
            autoEject = buffer.readBoolean(),
            networkProviderId = if (buffer.readBoolean()) buffer.readResourceLocation() else null,
        )
}

internal data class OutputOnlyJadeData(
    override val output: JadeOutputState,
) : IoMachineJadeData

internal object OutputOnlyJadeDataCodec : StreamCodec<RegistryFriendlyByteBuf, OutputOnlyJadeData> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: OutputOnlyJadeData,
    ) = JadeOutputStateCodec.encode(buffer, value.output)

    override fun decode(buffer: RegistryFriendlyByteBuf): OutputOnlyJadeData = OutputOnlyJadeData(JadeOutputStateCodec.decode(buffer))
}

/** Common entity lookup, codec and UID plumbing shared by every machine data provider. */
internal abstract class MachineJadeDataProvider<E : BlockEntity, D>(
    private val entityClass: Class<E>,
    private val uid: ResourceLocation,
    private val codec: StreamCodec<RegistryFriendlyByteBuf, D>,
) : StreamServerDataProvider<BlockAccessor, D> {
    final override fun streamData(accessor: BlockAccessor): D? {
        val rawEntity = accessor.blockEntity ?: return null
        if (!entityClass.isInstance(rawEntity)) return null
        return createData(entityClass.cast(rawEntity))
    }

    protected abstract fun createData(entity: E): D?

    final override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, D> = codec

    final override fun getUid(): ResourceLocation = uid
}

/**
 * IO-managed specialization. Subclasses only supply their own fields; output state collection and
 * transport are guaranteed to stay consistent for future machines.
 */
internal abstract class IoMachineJadeDataProvider<E : IoManagedBlockEntity, D : IoMachineJadeData>(
    entityClass: Class<E>,
    uid: ResourceLocation,
    codec: StreamCodec<RegistryFriendlyByteBuf, D>,
) : MachineJadeDataProvider<E, D>(entityClass, uid, codec) {
    final override fun createData(entity: E): D {
        val configuration = entity.ioController.configuration
        val output =
            JadeOutputState(
                mode = configuration.mode,
                autoEject = configuration.autoEject,
                networkProviderId = configuration.networkTarget?.providerId,
            )
        return createData(entity, output)
    }

    protected abstract fun createData(
        entity: E,
        output: JadeOutputState,
    ): D
}
