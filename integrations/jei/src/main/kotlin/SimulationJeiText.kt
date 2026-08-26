package rhx.lazy.integration.jei

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

internal fun groupTooltip(group: ResourceLocation): Component = Component.translatable("jei.lazy.simulation.group", group.toString())
