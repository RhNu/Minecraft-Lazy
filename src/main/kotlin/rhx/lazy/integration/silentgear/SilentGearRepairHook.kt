package rhx.lazy.integration.silentgear

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.silentchaos512.gear.util.GearData
import net.silentchaos512.gear.util.GearHelper
import rhx.lazy.feature.repairer.ItemRepairHook
import rhx.lazy.feature.repairer.ItemRepairHookResult

internal object SilentGearRepairHook : ItemRepairHook {
    override fun afterRepair(
        stack: ItemStack,
        player: Player?,
    ): ItemRepairHookResult {
        if (GearHelper.isGear(stack)) {
            GearData.recalculateGearData(stack, player)
        }
        return ItemRepairHookResult.Success
    }
}
