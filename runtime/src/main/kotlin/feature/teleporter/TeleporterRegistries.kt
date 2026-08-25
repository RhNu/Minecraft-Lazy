package rhx.lazy.feature.teleporter

import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.integration.api.LazyInternalApi
import java.util.function.Supplier

@LazyInternalApi
public object TeleporterRegistries : RegistryModule {
    private val items = DeferredRegister.createItems(MOD_ID)
    private val attachments: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID)

    val item =
        items.register(
            "teleporter",
            Supplier {
                TeleporterItem(
                    Item.Properties().apply {
                        stacksTo(1)
                        rarity(Rarity.EPIC)
                        fireResistant()
                    },
                )
            },
        )
    internal val playerState: Supplier<AttachmentType<TeleporterPlayerState>> =
        attachments.register(
            "teleporter_player_state",
            Supplier {
                AttachmentType
                    .builder(Supplier { TeleporterPlayerState.EMPTY })
                    .serialize(TeleporterPlayerState.CODEC)
                    .copyOnDeath()
                    .build()
            },
        )

    override fun register(bus: IEventBus) {
        items.register(bus)
        attachments.register(bus)
    }
}
