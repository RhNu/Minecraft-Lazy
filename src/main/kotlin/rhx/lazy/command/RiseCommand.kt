package rhx.lazy.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

internal object RiseCommand : LazySubcommand {
    override fun attachTo(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.then(
            Commands
                .literal("rise")
                .executes(::execute),
        )
    }

    private fun execute(context: CommandContext<CommandSourceStack>): Int {
        val player = context.source.entity as? ServerPlayer
        if (player == null) {
            context.source.sendFailure(Component.translatable("message.lazy.rise.player_only"))
            return 0
        }

        val target =
            RiseTargetFinder.find(
                player.serverLevel(),
                player.blockPosition().below(),
            )
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.lazy.rise.not_found"), true)
            return 0
        }

        teleport(player, target)
        player.displayClientMessage(Component.translatable("message.lazy.rise.success"), true)
        return Command.SINGLE_SUCCESS
    }

    private fun teleport(
        player: ServerPlayer,
        support: BlockPos,
    ) {
        val destination = Vec3.atCenterOf(support).add(0.0, 1.0, 0.0)
        player.teleportTo(
            player.serverLevel(),
            destination.x,
            destination.y,
            destination.z,
            player.yRot,
            player.xRot,
        )
    }
}
