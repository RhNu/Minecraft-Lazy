package rhx.lazy.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import rhx.lazy.util.lazyId

internal data object ActivateTeleporterPayload : CustomPacketPayload {
    val type = CustomPacketPayload.Type<ActivateTeleporterPayload>(lazyId("activate_teleporter"))
    val streamCodec: StreamCodec<RegistryFriendlyByteBuf, ActivateTeleporterPayload> = StreamCodec.unit(this)

    override fun type(): CustomPacketPayload.Type<ActivateTeleporterPayload> = type
}
