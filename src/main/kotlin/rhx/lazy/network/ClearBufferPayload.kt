package rhx.lazy.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import rhx.lazy.util.lazyId

internal data class ClearBufferPayload(
    val containerId: Int,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ClearBufferPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ClearBufferPayload>(lazyId("clear_buffer"))
        val STREAM_CODEC: StreamCodec<ByteBuf, ClearBufferPayload> =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ClearBufferPayload::containerId,
                ::ClearBufferPayload,
            )
    }
}
