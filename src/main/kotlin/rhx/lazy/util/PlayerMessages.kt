package rhx.lazy.util

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

internal fun Player.displayActionBar(
    translationKey: String,
    vararg args: Any,
) {
    displayClientMessage(Component.translatable(translationKey, *args), true)
}
