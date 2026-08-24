package rhx.lazy.core

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public fun Player.displayActionBar(
    translationKey: String,
    vararg args: Any,
) {
    displayClientMessage(Component.translatable(translationKey, *args), true)
}
