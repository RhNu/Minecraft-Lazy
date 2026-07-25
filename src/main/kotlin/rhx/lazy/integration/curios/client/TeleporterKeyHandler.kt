package rhx.lazy.integration.curios.client

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.network.PacketDistributor
import rhx.lazy.MOD_ID
import rhx.lazy.integration.curios.ActivateEquippedTeleporterPayload

@EventBusSubscriber(modid = MOD_ID, value = [Dist.CLIENT])
internal object TeleporterKeyMappings {
    val activate =
        KeyMapping(
            "key.lazy.teleporter.activate",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.value,
            "key.categories.lazy",
        )

    @SubscribeEvent
    fun register(event: RegisterKeyMappingsEvent) {
        event.register(activate)
    }
}

@EventBusSubscriber(modid = MOD_ID, value = [Dist.CLIENT])
internal object TeleporterKeyHandler {
    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        if (Minecraft.getInstance().player == null) return

        while (TeleporterKeyMappings.activate.consumeClick()) {
            PacketDistributor.sendToServer(ActivateEquippedTeleporterPayload)
        }
    }
}
