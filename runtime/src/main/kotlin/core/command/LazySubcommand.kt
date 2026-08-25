package rhx.lazy.core.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import rhx.lazy.integration.api.LazyInternalApi

/** Adds one subcommand to the shared `/lazy` command root. */
@LazyInternalApi
public fun interface LazySubcommand {
    public fun attachTo(root: LiteralArgumentBuilder<CommandSourceStack>)
}
