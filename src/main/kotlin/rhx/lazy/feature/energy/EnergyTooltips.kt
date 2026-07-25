package rhx.lazy.feature.energy

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

internal fun MutableList<Component>.addEnergyTransferTooltip(transferLimit: Int) {
    add(
        Component
            .translatable(
                "tooltip.lazy.energy.max_transfer",
                Component.literal(transferLimit.toString()).withStyle(ChatFormatting.AQUA),
            ).withStyle(ChatFormatting.GRAY),
    )
}
