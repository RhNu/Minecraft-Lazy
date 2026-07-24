package rhx.lazy.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import rhx.lazy.protection.DamageCapData
import rhx.lazy.registry.ModAttachments
import rhx.lazy.util.displayActionBar

internal object ProtectionCommand : LazySubcommand {
    private const val PERMISSION_LEVEL = 2

    override fun attachTo(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.then(
            Commands
                .literal("protection")
                .requires { it.hasPermission(PERMISSION_LEVEL) }
                .then(
                    Commands
                        .literal("damage_cap")
                        .executes(::showStatus)
                        .then(Commands.literal("on").executes(::enable))
                        .then(Commands.literal("off").executes(::disable))
                        .then(
                            Commands
                                .literal("set")
                                .then(
                                    Commands
                                        .argument("value", IntegerArgumentType.integer(0))
                                        .executes(::setThreshold),
                                ),
                        ).then(Commands.literal("reset").executes(::reset)),
                ),
        )
    }

    private fun showStatus(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            val data = player.getExistingDataOrNull(ModAttachments.damageCap)
            val state =
                if (data?.enabled == true) {
                    Component.translatable("message.lazy.protection.damage_cap.enabled")
                } else {
                    Component.translatable("message.lazy.protection.damage_cap.disabled")
                }
            context.source.sendSuccess(
                {
                    Component.translatable(
                        "message.lazy.protection.damage_cap.status",
                        state,
                        data?.threshold ?: 0,
                    )
                },
                false,
            )
        }

    private fun enable(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            updateData(player) { it.copy(enabled = true) }
            player.displayActionBar("message.lazy.protection.damage_cap.on")
        }

    private fun disable(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            player.getExistingDataOrNull(ModAttachments.damageCap)?.let { data ->
                player.setData(ModAttachments.damageCap, data.copy(enabled = false))
            }
            player.displayActionBar("message.lazy.protection.damage_cap.off")
        }

    private fun setThreshold(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            val value = IntegerArgumentType.getInteger(context, "value")
            updateData(player) { it.copy(threshold = value) }
            player.displayActionBar("message.lazy.protection.damage_cap.set", value)
        }

    private fun reset(context: CommandContext<CommandSourceStack>): Int =
        context.withServerPlayer(PLAYER_ONLY) { player ->
            player.removeData(ModAttachments.damageCap)
            player.displayActionBar("message.lazy.protection.damage_cap.reset")
        }

    private inline fun updateData(
        player: ServerPlayer,
        transform: (DamageCapData) -> DamageCapData,
    ) {
        val current = player.getExistingDataOrNull(ModAttachments.damageCap) ?: DamageCapData()
        player.setData(ModAttachments.damageCap, transform(current))
    }

    private const val PLAYER_ONLY = "message.lazy.protection.damage_cap.player_only"
}
