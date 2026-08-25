package rhx.lazy.core.command

import net.minecraft.commands.Commands
import net.neoforged.neoforge.event.RegisterCommandsEvent
import rhx.lazy.MOD_ID
import rhx.lazy.feature.protection.ProtectionCommand
import rhx.lazy.feature.rise.RiseCommand
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public object LazyCommands {
    private val subcommands: MutableMap<String, LazySubcommand> =
        linkedMapOf(
            "rise" to RiseCommand,
            "protection" to ProtectionCommand,
        )

    public fun contribute(
        id: String,
        subcommand: LazySubcommand,
    ) {
        require(id.matches(Regex("[a-z0-9_.-]+"))) { "Invalid Lazy subcommand contribution id: $id" }
        synchronized(subcommands) {
            val existing = subcommands.putIfAbsent(id, subcommand)
            check(existing == null || existing === subcommand) {
                "Lazy subcommand contribution '$id' is already registered"
            }
        }
    }

    internal fun register(event: RegisterCommandsEvent) {
        val root = Commands.literal(MOD_ID)
        synchronized(subcommands) {
            subcommands.values.forEach { subcommand -> subcommand.attachTo(root) }
        }
        event.dispatcher.register(root)
    }
}
