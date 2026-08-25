package rhx.lazy.integration.tacz

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import rhx.lazy.core.command.LazySubcommand
import rhx.lazy.core.command.withServerPlayer
import rhx.lazy.core.displayActionBar

internal object TaczCommand : LazySubcommand {
    private const val PERMISSION_LEVEL = 2
    private const val PLAYER_ONLY = "message.lazy.tacz.infammo.player_only"

    override fun attachTo(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.then(
            Commands
                .literal("tacz")
                .requires { it.hasPermission(PERMISSION_LEVEL) }
                .then(
                    Commands
                        .literal("infammo")
                        .executes(::showStatus)
                        .then(Commands.literal("on").executes(::enable))
                        .then(Commands.literal("off").executes(::disable))
                        .then(Commands.literal("reset").executes(::reset)),
                ),
        )
    }

    private fun showStatus(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            val state =
                if (TaczInfiniteAmmoState.isEnabled(player)) {
                    Component.translatable("message.lazy.tacz.infammo.enabled")
                } else {
                    Component.translatable("message.lazy.tacz.infammo.disabled")
                }
            context.source.sendSuccess(
                { Component.translatable("message.lazy.tacz.infammo.status", state) },
                false,
            )
        }

    private fun enable(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            TaczInfiniteAmmoState.setEnabled(player, true)
            player.displayActionBar("message.lazy.tacz.infammo.on")
        }

    private fun disable(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            TaczInfiniteAmmoState.setEnabled(player, false)
            player.displayActionBar("message.lazy.tacz.infammo.off")
        }

    private fun reset(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            TaczInfiniteAmmoState.setEnabled(player, false)
            player.displayActionBar("message.lazy.tacz.infammo.reset")
        }
}
