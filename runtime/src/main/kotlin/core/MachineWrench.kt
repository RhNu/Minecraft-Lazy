package rhx.lazy.core

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

/**
 * Wrench interactions for Lazy's machines: sneak-use takes the machine apart, plain use turns it.
 *
 * This runs on [PlayerInteractEvent.RightClickBlock] rather than on the block, because vanilla skips
 * a block's own item interaction entirely while the player sneaks with something in hand — which is
 * exactly the half of this feature that needs to work.
 */
internal object MachineWrench {
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (!event.itemStack.`is`(Tags.Items.TOOLS_WRENCH)) return
        val level = event.level
        val pos = event.pos
        val state = level.getBlockState(pos)
        val block = state.block as? MachineBlock ?: return
        val player = event.entity
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return

        event.isCanceled = true
        event.cancellationResult = InteractionResult.sidedSuccess(level.isClientSide)

        val serverLevel = level as? ServerLevel ?: return
        if (player.isSecondaryUseActive) {
            block.dismantle(serverLevel, pos, player)
        } else {
            block.rotateClockwise(serverLevel, pos, state)
        }
    }
}
