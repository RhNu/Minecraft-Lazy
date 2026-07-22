package rhx.lazy.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

/** Adds one subcommand to the shared `/lazy` command root. */
internal fun interface LazySubcommand {
    fun attachTo(root: LiteralArgumentBuilder<CommandSourceStack>)
}
