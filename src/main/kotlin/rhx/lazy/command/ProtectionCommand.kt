package rhx.lazy.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import rhx.lazy.protection.DamageCapData
import rhx.lazy.registry.ModAttachments

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
        withPlayer(context) { player ->
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
        withPlayer(context) { player ->
            updateData(player) { it.copy(enabled = true) }
            player.displayClientMessage(Component.translatable("message.lazy.protection.damage_cap.on"), true)
        }

    private fun disable(context: CommandContext<CommandSourceStack>): Int =
        withPlayer(context) { player ->
            player.getExistingDataOrNull(ModAttachments.damageCap)?.let { data ->
                player.setData(ModAttachments.damageCap, data.copy(enabled = false))
            }
            player.displayClientMessage(Component.translatable("message.lazy.protection.damage_cap.off"), true)
        }

    private fun setThreshold(context: CommandContext<CommandSourceStack>): Int =
        withPlayer(context) { player ->
            val value = IntegerArgumentType.getInteger(context, "value")
            updateData(player) { it.copy(threshold = value) }
            player.displayClientMessage(Component.translatable("message.lazy.protection.damage_cap.set", value), true)
        }

    private fun reset(context: CommandContext<CommandSourceStack>): Int =
        withPlayer(context) { player ->
            player.removeData(ModAttachments.damageCap)
            player.displayClientMessage(Component.translatable("message.lazy.protection.damage_cap.reset"), true)
        }

    private inline fun withPlayer(
        context: CommandContext<CommandSourceStack>,
        action: (ServerPlayer) -> Unit,
    ): Int {
        val player = context.source.entity as? ServerPlayer
        if (player == null) {
            context.source.sendFailure(Component.translatable("message.lazy.protection.damage_cap.player_only"))
            return 0
        }

        action(player)
        return Command.SINGLE_SUCCESS
    }

    private inline fun updateData(
        player: ServerPlayer,
        transform: (DamageCapData) -> DamageCapData,
    ) {
        val current = player.getExistingDataOrNull(ModAttachments.damageCap) ?: DamageCapData()
        player.setData(ModAttachments.damageCap, transform(current))
    }
}
