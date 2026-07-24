package rhx.lazy.registry

import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import rhx.lazy.MOD_ID
import rhx.lazy.protection.DamageCapData
import java.util.function.Supplier

internal object ModAttachments : RegistryModule {
    private val registry = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID)

    val damageCap: Supplier<AttachmentType<DamageCapData>> =
        registry.register(
            "damage_cap",
            Supplier {
                AttachmentType
                    .builder(::DamageCapData)
                    .serialize(DamageCapData.CODEC)
                    .copyOnDeath()
                    .build()
            },
        )

    override fun register(bus: net.neoforged.bus.api.IEventBus) {
        registry.register(bus)
    }
}
