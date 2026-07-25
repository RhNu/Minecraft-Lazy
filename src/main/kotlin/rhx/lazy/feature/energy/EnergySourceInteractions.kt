package rhx.lazy.feature.energy

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import rhx.lazy.core.sidedUseResult

internal object EnergySourceInteractions {
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entity
        if (!player.isShiftKeyDown) return

        val level = event.level
        val block = level.getBlockState(event.pos).block as? EnergySourceBlock ?: return
        if (!block.handleUse(level, event.pos, player)) return

        event.setCancellationResult(level.sidedUseResult(handled = true))
        event.setCanceled(true)
    }
}
