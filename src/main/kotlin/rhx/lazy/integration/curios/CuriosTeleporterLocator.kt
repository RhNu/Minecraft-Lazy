package rhx.lazy.integration.curios

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import rhx.lazy.feature.teleporter.TeleporterRegistries
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosTeleporterLocator {
    fun findEquipped(player: ServerPlayer): ItemStack? =
        CuriosApi
            .getCuriosInventory(player)
            .flatMap { inventory ->
                inventory.findCurio(
                    CuriosTeleporterIntegration.TELEPORTER_SLOT,
                    CuriosTeleporterIntegration.TELEPORTER_SLOT_INDEX,
                )
            }.map { result -> result.stack() }
            .filter { stack -> stack.item === TeleporterRegistries.item.get() }
            .orElse(null)
}
