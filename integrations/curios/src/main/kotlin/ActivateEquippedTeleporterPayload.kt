package rhx.lazy.integration.curios

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import rhx.lazy.core.lazyId

internal data object ActivateEquippedTeleporterPayload : CustomPacketPayload {
    val type = CustomPacketPayload.Type<ActivateEquippedTeleporterPayload>(lazyId("activate_teleporter"))
    val streamCodec: StreamCodec<RegistryFriendlyByteBuf, ActivateEquippedTeleporterPayload> = StreamCodec.unit(this)

    override fun type(): CustomPacketPayload.Type<ActivateEquippedTeleporterPayload> = type
}
