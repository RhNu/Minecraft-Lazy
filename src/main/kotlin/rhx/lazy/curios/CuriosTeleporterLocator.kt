package rhx.lazy.curios

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import rhx.lazy.registry.ModItems
import top.theillusivec4.curios.api.CuriosApi

internal object CuriosTeleporterLocator {
    fun findEquipped(player: ServerPlayer): ItemStack? =
        CuriosApi
            .getCuriosInventory(player)
            .flatMap { inventory ->
                inventory.findCurio(
                    CuriosIntegration.TELEPORTER_SLOT,
                    CuriosIntegration.TELEPORTER_SLOT_INDEX,
                )
            }.map { result -> result.stack() }
            .filter { stack -> stack.item === ModItems.teleporter.get() }
            .orElse(null)
}
