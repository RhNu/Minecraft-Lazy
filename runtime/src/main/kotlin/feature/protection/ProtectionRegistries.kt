package rhx.lazy.feature.protection

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import java.util.function.Supplier

internal object ProtectionRegistries : RegistryModule {
    private val attachments = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID)

    val damageCap: Supplier<AttachmentType<DamageCapData>> =
        attachments.register(
            "damage_cap",
            Supplier {
                AttachmentType
                    .builder(::DamageCapData)
                    .serialize(DamageCapData.CODEC)
                    .copyOnDeath()
                    .build()
            },
        )

    override fun register(bus: IEventBus) {
        attachments.register(bus)
    }
}
