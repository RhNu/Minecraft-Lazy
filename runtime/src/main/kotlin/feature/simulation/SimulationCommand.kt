package rhx.lazy.feature.simulation

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.command.LazySubcommand

internal object SimulationCommand : LazySubcommand {
    override fun attachTo(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.then(
            Commands
                .literal("simulation")
                .then(
                    Commands
                        .literal("inspect")
                        .then(Commands.literal("held").executes(::inspectHeld))
                        .then(
                            Commands
                                .literal("chamber")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(::inspectChamber)),
                        ),
                ),
        )
    }

    private fun inspectHeld(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val target = source.playerOrException.mainHandItem
        return report(source, target, emptyList())
    }

    private fun inspectChamber(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val pos = BlockPosArgument.getLoadedBlockPos(context, "pos")
        val chamber = source.level.getBlockEntity(pos) as? SimulationChamberBlockEntity
        if (chamber == null) {
            source.sendFailure(Component.translatable("command.lazy.simulation.inspect.not_chamber", pos.x, pos.y, pos.z))
            return 0
        }
        return report(
            source,
            chamber.getInput(SimulationChamberBlockEntity.TARGET_SLOT),
            List(SimulationChamberBlockEntity.TOOL_SLOTS) { index ->
                chamber.getInput(SimulationChamberBlockEntity.TOOL_SLOT_START + index)
            },
        )
    }

    private fun report(
        source: CommandSourceStack,
        target: ItemStack,
        tools: List<ItemStack>,
    ): Int {
        val inspection = SimulationRecipeResolver.inspect(source.level, target, tools)
        source.sendSuccess(
            {
                Component.translatable(
                    "command.lazy.simulation.inspect.header",
                    target.hoverName,
                    tools.filterNot(ItemStack::isEmpty).joinToString { BuiltInRegistries.ITEM.getKey(it.item).toString() },
                )
            },
            false,
        )
        inspection.candidates.forEach { candidate ->
            val status =
                if (candidate.toolsMatch) {
                    Component.translatable("command.lazy.simulation.inspect.matches")
                } else {
                    Component.translatable("command.lazy.simulation.inspect.missing_tools")
                }
            source.sendSuccess(
                {
                    Component.translatable(
                        "command.lazy.simulation.inspect.candidate",
                        candidate.kind,
                        candidate.id,
                        candidate.group,
                        candidate.priority,
                        candidate.tools.size,
                        status,
                    )
                },
                false,
            )
        }
        when (val resolution = inspection.resolution) {
            is SimulationResolution.Success ->
                source.sendSuccess(
                    {
                        Component.translatable(
                            "command.lazy.simulation.inspect.selected",
                            resolution.simulation.id,
                            resolution.simulation.group,
                        )
                    },
                    false,
                )
            is SimulationResolution.Conflict ->
                source.sendFailure(
                    Component.translatable("command.lazy.simulation.inspect.conflict", resolution.ids.joinToString()),
                )
            SimulationResolution.Unavailable ->
                source.sendFailure(Component.translatable("command.lazy.simulation.inspect.unavailable"))
        }
        return if (inspection.resolution is SimulationResolution.Success) 1 else 0
    }
}
