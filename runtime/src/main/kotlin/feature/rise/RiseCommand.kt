package rhx.lazy.feature.rise

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import rhx.lazy.core.command.LazySubcommand
import rhx.lazy.core.command.serverPlayerOrNull
import rhx.lazy.core.displayActionBar

internal object RiseCommand : LazySubcommand {
    override fun attachTo(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.then(
            Commands
                .literal("rise")
                .executes(::execute),
        )
    }

    private fun execute(context: CommandContext<CommandSourceStack>): Int {
        val player = context.serverPlayerOrNull(PLAYER_ONLY) ?: return 0

        val target =
            RiseTargetFinder.find(
                player.serverLevel(),
                player.blockPosition(),
            )
        if (target == null) {
            player.displayActionBar("message.lazy.rise.not_found")
            return 0
        }

        teleport(player, target)
        player.displayActionBar("message.lazy.rise.success")
        return Command.SINGLE_SUCCESS
    }

    private fun teleport(
        player: ServerPlayer,
        destination: Vec3,
    ) {
        player.teleportTo(
            player.serverLevel(),
            destination.x,
            destination.y,
            destination.z,
            player.yRot,
            player.xRot,
        )
    }

    private const val PLAYER_ONLY = "message.lazy.rise.player_only"
}
