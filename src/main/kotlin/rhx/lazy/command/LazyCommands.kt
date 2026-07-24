package rhx.lazy.command

import net.minecraft.commands.Commands
import net.neoforged.neoforge.event.RegisterCommandsEvent
import rhx.lazy.MOD_ID

internal object LazyCommands {
    private val subcommands: List<LazySubcommand> =
        listOf(
            RiseCommand,
            ProtectionCommand,
        )

    fun register(event: RegisterCommandsEvent) {
        val root = Commands.literal(MOD_ID)
        subcommands.forEach { subcommand -> subcommand.attachTo(root) }
        event.dispatcher.register(root)
    }
}
