package rhx.lazy.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import rhx.lazy.block.entity.BufferSnapshot
import rhx.lazy.util.lazyId

internal data class BufferContentsPayload(
    val containerId: Int,
    val snapshot: BufferSnapshot,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<BufferContentsPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<BufferContentsPayload>(lazyId("buffer_contents"))
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BufferContentsPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, BufferContentsPayload> {
                override fun decode(buffer: RegistryFriendlyByteBuf): BufferContentsPayload =
                    BufferContentsPayload(buffer.readVarInt(), BufferSnapshot.read(buffer))

                override fun encode(
                    buffer: RegistryFriendlyByteBuf,
                    payload: BufferContentsPayload,
                ) {
                    buffer.writeVarInt(payload.containerId)
                    payload.snapshot.write(buffer)
                }
            }
    }
}
