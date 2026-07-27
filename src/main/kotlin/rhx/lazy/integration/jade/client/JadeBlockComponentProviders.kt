package rhx.lazy.integration.jade.client

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.feature.energy.EnergyOutputMode
import rhx.lazy.integration.jade.BufferJadeDataProvider
import rhx.lazy.integration.jade.EnergySourceJadeDataProvider
import rhx.lazy.integration.jade.ItemCopierJadeDataProvider
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.RepairerJadeDataProvider
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig
import snownee.jade.api.ui.IElementHelper

internal object BufferJadeComponentProvider : IBlockComponentProvider {
    override fun appendTooltip(
        tooltip: ITooltip,
        accessor: BlockAccessor,
        config: IPluginConfig,
    ) {
        val data = BufferJadeDataProvider.decodeFromData(accessor).orElse(null) ?: return
        tooltip.add(
            Component.translatable(
                "jade.lazy.buffer.contents",
                data.itemCount,
                BufferBlockEntity.TOTAL_ITEM_CAPACITY,
                data.fluidAmount,
                BufferBlockEntity.TOTAL_FLUID_CAPACITY,
            ),
        )
        tooltip.add(
            Component.translatable(
                "jade.lazy.buffer.network_forwarding",
                enabledState(data.networkForwarding),
            ),
        )
    }

    override fun getUid(): ResourceLocation = JadeProviderIds.buffer
}

internal object EnergySourceJadeComponentProvider : IBlockComponentProvider {
    override fun appendTooltip(
        tooltip: ITooltip,
        accessor: BlockAccessor,
        config: IPluginConfig,
    ) {
        val mode = EnergySourceJadeDataProvider.decodeFromData(accessor).orElse(null) ?: return
        tooltip.add(
            Component.translatable(
                "jade.lazy.energy_source.output_mode",
                Component.translatable(mode.translationKey()),
            ),
        )
    }

    override fun getUid(): ResourceLocation = JadeProviderIds.energySource
}

internal object ItemCopierJadeComponentProvider : IBlockComponentProvider {
    override fun appendTooltip(
        tooltip: ITooltip,
        accessor: BlockAccessor,
        config: IPluginConfig,
    ) {
        val data = ItemCopierJadeDataProvider.decodeFromData(accessor).orElse(null) ?: return
        if (data.template.isEmpty) {
            tooltip.add(Component.translatable("gui.lazy.item_copier.template.empty"))
        } else {
            tooltip.add(IElementHelper.get().smallItem(data.template))
            tooltip.append(
                Component.translatable(
                    "jade.lazy.item_copier.template",
                    data.template.hoverName,
                ),
            )
        }
        tooltip.add(Component.translatable("tooltip.lazy.item_copier.interval", data.intervalTicks))
    }

    override fun getUid(): ResourceLocation = JadeProviderIds.itemCopier
}

internal object RepairerJadeComponentProvider : IBlockComponentProvider {
    override fun appendTooltip(
        tooltip: ITooltip,
        accessor: BlockAccessor,
        config: IPluginConfig,
    ) {
        val stack = RepairerJadeDataProvider.decodeFromData(accessor).orElse(null) ?: return
        if (stack.isEmpty) return

        tooltip.add(IElementHelper.get().smallItem(stack))
        tooltip.append(Component.translatable("jade.lazy.repairer.item", stack.hoverName))
        if (stack.isDamageableItem) {
            tooltip.add(
                Component.translatable(
                    "jade.lazy.repairer.durability",
                    (stack.maxDamage - stack.damageValue).coerceAtLeast(0),
                    stack.maxDamage,
                ),
            )
        }
    }

    override fun getUid(): ResourceLocation = JadeProviderIds.repairer
}

private fun EnergyOutputMode.translationKey(): String =
    when (this) {
        EnergyOutputMode.PASSIVE -> "gui.lazy.energy_source.passive"
        EnergyOutputMode.ACTIVE -> "gui.lazy.energy_source.active"
        EnergyOutputMode.NETWORK -> "gui.lazy.energy_source.network"
    }

internal fun enabledState(enabled: Boolean): Component =
    Component.translatable(
        if (enabled) {
            "jade.lazy.enabled"
        } else {
            "jade.lazy.disabled"
        },
    )
