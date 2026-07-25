package rhx.lazy.integration.repair

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

internal fun interface ItemRepairCompatibility {
    fun afterRepair(
        stack: ItemStack,
        player: Player?,
    )
}
