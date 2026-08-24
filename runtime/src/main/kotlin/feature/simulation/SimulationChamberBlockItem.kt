package rhx.lazy.feature.simulation

import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public class SimulationChamberBlockItem(
    block: net.minecraft.world.level.block.Block,
    properties: Properties,
) : BlockItem(block, properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag,
    ) {
        super.appendHoverText(stack, context, tooltip, flag)
        if (stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)) {
            tooltip += Component.translatable("tooltip.lazy.simulation_chamber.contents")
        }
    }
}
