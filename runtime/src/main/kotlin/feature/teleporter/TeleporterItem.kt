package rhx.lazy.feature.teleporter

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class TeleporterItem(
    properties: Properties,
) : Item(properties) {
    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand,
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (!level.isClientSide) {
            val serverPlayer = player as? ServerPlayer ?: return InteractionResultHolder.fail(stack)
            TeleporterUI.open(serverPlayer)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }
}
