package rhx.lazy.block

import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.level.Level

internal fun Level.sidedUseResult(handled: Boolean): InteractionResult =
    if (handled) {
        InteractionResult.sidedSuccess(isClientSide)
    } else {
        InteractionResult.PASS
    }

internal fun Level.sidedItemUseResult(handled: Boolean): ItemInteractionResult =
    if (handled) {
        ItemInteractionResult.sidedSuccess(isClientSide)
    } else {
        ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }
