package rhx.lazy.integration.jade.client

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.integration.jade.BufferJadeDataProvider
import rhx.lazy.integration.jade.EnergySourceJadeDataProvider
import rhx.lazy.integration.jade.ItemCopierJadeDataProvider
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.RepairerJadeDataProvider
import rhx.lazy.integration.jade.SimulationChamberJadeDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.ui.BoxStyle
import snownee.jade.api.ui.IElementHelper

internal object BufferJadeComponentProvider :
    IoMachineJadeComponentProvider<rhx.lazy.integration.jade.BufferJadeData>(
        BufferJadeDataProvider,
        JadeProviderIds.buffer,
    ) {
    override fun appendBeforeOutput(
        tooltip: ITooltip,
        data: rhx.lazy.integration.jade.BufferJadeData,
    ) {
        tooltip.add(
            Component.translatable(
                "jade.lazy.buffer.contents",
                data.itemCount,
                BufferBlockEntity.TOTAL_ITEM_CAPACITY,
                data.fluidAmount,
                BufferBlockEntity.TOTAL_FLUID_CAPACITY,
            ),
        )
    }
}

internal object EnergySourceJadeComponentProvider :
    IoMachineJadeComponentProvider<rhx.lazy.integration.jade.OutputOnlyJadeData>(
        EnergySourceJadeDataProvider,
        JadeProviderIds.energySource,
    )

internal object ItemCopierJadeComponentProvider :
    IoMachineJadeComponentProvider<rhx.lazy.integration.jade.ItemCopierJadeData>(
        ItemCopierJadeDataProvider,
        JadeProviderIds.itemCopier,
    ) {
    override fun appendBeforeOutput(
        tooltip: ITooltip,
        data: rhx.lazy.integration.jade.ItemCopierJadeData,
    ) {
        if (data.template.isEmpty) {
            tooltip.add(Component.translatable("jade.lazy.item_copier.unmarked_output"))
        } else {
            tooltip.add(IElementHelper.get().smallItem(data.template))
            tooltip.append(data.template.hoverName)
        }
        tooltip.add(Component.translatable("jade.lazy.item_copier.generation_interval", data.intervalTicks))
    }
}

internal object RepairerJadeComponentProvider :
    MachineJadeComponentProvider<ItemStack>(
        RepairerJadeDataProvider,
        JadeProviderIds.repairer,
    ) {
    override fun appendTooltip(
        tooltip: ITooltip,
        data: ItemStack,
    ) {
        val stack = data
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
}

internal object SimulationChamberJadeComponentProvider :
    IoMachineJadeComponentProvider<rhx.lazy.integration.jade.SimulationChamberJadeData>(
        SimulationChamberJadeDataProvider,
        JadeProviderIds.simulationChamber,
    ) {
    override fun appendBeforeOutput(
        tooltip: ITooltip,
        data: rhx.lazy.integration.jade.SimulationChamberJadeData,
    ) {
        val progress = data.progress.coerceIn(0f, 1f)
        val elements = IElementHelper.get()
        tooltip.add(
            Component.translatable("jade.lazy.simulation_chamber.progress", (progress * 100).toInt()),
        )
        tooltip.add(
            elements.progress(
                progress,
                null,
                elements.progressStyle(),
                BoxStyle.getNestedBox(),
                true,
            ),
        )
        tooltip.add(Component.translatable("jade.lazy.simulation_chamber.multipliers", data.speed, data.outputMultiplier))
        if (data.pending) tooltip.add(Component.translatable("jade.lazy.simulation_chamber.pending"))
    }
}
