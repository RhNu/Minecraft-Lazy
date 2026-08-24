package rhx.lazy.core.configurator

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import rhx.lazy.core.displayActionBar

internal class ModularConfiguratorItem(
    properties: Properties,
) : Item(properties),
    HeldItemUIMenuType.HeldItemUI {
    override fun onItemUseFirst(
        stack: ItemStack,
        context: UseOnContext,
    ): InteractionResult = ModularConfiguratorModules.useOn(context) ?: InteractionResult.PASS

    override fun useOn(context: UseOnContext): InteractionResult {
        ModularConfiguratorModules.useOn(context)?.let { return it }
        val player = context.player ?: return InteractionResult.PASS
        return open(context.level, player, context.hand)
    }

    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand,
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (player.isShiftKeyDown) {
            if (!level.isClientSide && player is ServerPlayer) {
                val cleared = ModularConfiguratorDataAccess.clearModules(stack)
                player.displayActionBar(
                    if (cleared) {
                        "message.lazy.modular_configurator.cleared"
                    } else {
                        "message.lazy.modular_configurator.nothing_to_clear"
                    },
                )
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
        }
        val result = open(level, player, usedHand)
        return InteractionResultHolder(result, stack)
    }

    override fun createUI(holder: HeldItemUIMenuType.HeldItemUIHolder): ModularUI = ModularConfiguratorUI.create(holder)

    private fun open(
        level: Level,
        player: Player,
        hand: InteractionHand,
    ): InteractionResult {
        if (!level.isClientSide) {
            val serverPlayer = player as? ServerPlayer ?: return InteractionResult.FAIL
            if (!HeldItemUIMenuType.openUI(serverPlayer, hand)) return InteractionResult.FAIL
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
}
