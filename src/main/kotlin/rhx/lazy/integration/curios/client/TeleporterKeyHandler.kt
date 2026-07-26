package rhx.lazy.integration.curios.client

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.network.PacketDistributor
import rhx.lazy.integration.ClientIntegrationContext
import rhx.lazy.integration.curios.ActivateEquippedTeleporterPayload

internal object CuriosClientAdapter {
    fun initialize(context: ClientIntegrationContext) {
        context.modBus.addListener(TeleporterKeyMappings::register)
        context.gameBus.addListener(TeleporterKeyHandler::onClientTick)
    }
}

internal object TeleporterKeyMappings {
    val activate =
        KeyMapping(
            "key.lazy.teleporter.activate",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.value,
            "key.categories.lazy",
        )

    fun register(event: RegisterKeyMappingsEvent) {
        event.register(activate)
    }
}

internal object TeleporterKeyHandler {
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null) return

        while (TeleporterKeyMappings.activate.consumeClick()) {
            val connection = minecraft.connection ?: continue
            if (!connection.hasChannel(ActivateEquippedTeleporterPayload.type)) continue
            PacketDistributor.sendToServer(ActivateEquippedTeleporterPayload)
        }
    }
}
