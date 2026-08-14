package rhx.lazy.feature.simulation

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

internal class SimulationCoreItem(
    val tier: SimulationCoreTier,
    properties: Properties,
) : Item(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        tooltipComponents +=
            Component
                .translatable(
                    "tooltip.lazy.simulation_core",
                    tier.speedMultiplier(),
                    tier.outputMultiplier(),
                ).withStyle(ChatFormatting.GRAY)
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}
