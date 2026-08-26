package rhx.lazy.integration.apotheosis

import dev.shadowsoffire.apotheosis.affix.AffixHelper
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu
import dev.shadowsoffire.apotheosis.socket.gem.GemItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import rhx.lazy.feature.simulation.SimulationIncinerationHandler

internal object ApotheosisIncinerationHandler : SimulationIncinerationHandler {
    override fun claims(stack: ItemStack): Boolean = stack.item !is GemItem && AffixHelper.hasAffixes(stack)

    override fun process(
        level: ServerLevel,
        stack: ItemStack,
    ): List<ItemStack> = SalvagingMenu.getSalvageResults(level, stack)
}
