package rhx.lazy.integration.tacz

import com.mojang.serialization.Codec
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import rhx.lazy.MOD_ID
import java.util.function.Supplier

internal object TaczRegistries {
    private val attachments = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID)

    internal val infiniteAmmo: Supplier<AttachmentType<Boolean>> =
        attachments.register(
            "tacz_infinite_ammo",
            Supplier {
                AttachmentType
                    .builder(Supplier { false })
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .sync(
                        { holder, to -> holder === to },
                        ByteBufCodecs.BOOL,
                    ).build()
            },
        )

    fun register(bus: IEventBus) {
        attachments.register(bus)
    }
}
