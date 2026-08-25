package rhx.lazy.core.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import rhx.lazy.integration.api.LazyInternalApi

internal fun CommandContext<CommandSourceStack>.serverPlayerOrNull(failureTranslationKey: String): ServerPlayer? {
    val player = source.entity as? ServerPlayer
    if (player == null) {
        source.sendFailure(Component.translatable(failureTranslationKey))
    }
    return player
}

@LazyInternalApi
public fun CommandContext<CommandSourceStack>.withServerPlayer(
    failureTranslationKey: String,
    action: (ServerPlayer) -> Unit,
): Int {
    val player = serverPlayerOrNull(failureTranslationKey) ?: return 0
    action(player)
    return Command.SINGLE_SUCCESS
}
