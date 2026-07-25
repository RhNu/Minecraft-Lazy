package rhx.lazy.integration.silentgear

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.silentchaos512.gear.util.GearData
import net.silentchaos512.gear.util.GearHelper
import rhx.lazy.integration.repair.ItemRepairCompatibility

internal object SilentGearRepairCompatibility : ItemRepairCompatibility {
    override fun afterRepair(
        stack: ItemStack,
        player: Player?,
    ) {
        if (GearHelper.isGear(stack)) {
            GearData.recalculateGearData(stack, player)
        }
    }
}
