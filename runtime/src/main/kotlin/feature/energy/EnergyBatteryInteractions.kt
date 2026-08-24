package rhx.lazy.feature.energy

import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import rhx.lazy.core.displayActionBar

internal object EnergyBatteryInteractions {
    fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.action != PlayerInteractEvent.LeftClickBlock.Action.START) return
        if (event.itemStack.item !is EnergyBatteryItem) return

        val face = event.face ?: return
        val storage =
            event.level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                event.pos,
                face,
            ) ?: return
        if (!storage.canReceive()) return

        event.isCanceled = true
        if (event.level.isClientSide) return

        val transferred = storage.receiveEnergy(Int.MAX_VALUE, false)
        event.entity.displayActionBar("message.lazy.energy_battery.transfer", transferred)
    }
}
