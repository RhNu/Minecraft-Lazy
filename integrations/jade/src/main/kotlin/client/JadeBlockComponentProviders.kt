package rhx.lazy.integration.jade.client

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.integration.jade.BufferJadeDataProvider
import rhx.lazy.integration.jade.EnergySourceJadeDataProvider
import rhx.lazy.integration.jade.JadeProviderIds
import rhx.lazy.integration.jade.RepairerJadeDataProvider
import rhx.lazy.integration.jade.ReplicatorJadeDataProvider
import rhx.lazy.integration.jade.SimulationChamberJadeDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.fluid.JadeFluidObject
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

internal object ReplicatorJadeComponentProvider :
    IoMachineJadeComponentProvider<rhx.lazy.integration.jade.ReplicatorJadeData>(
        ReplicatorJadeDataProvider,
        JadeProviderIds.replicator,
    ) {
    override fun appendBeforeOutput(
        tooltip: ITooltip,
        data: rhx.lazy.integration.jade.ReplicatorJadeData,
    ) {
        val resource = data.resource
        if (resource == null) {
            tooltip.add(Component.translatable("jade.lazy.replicator.unmarked_output"))
        } else {
            val elements = IElementHelper.get()
            when (val variant = resource.variant) {
                is ItemVariant -> tooltip.add(elements.smallItem(variant.template))
                is FluidVariant -> {
                    val template = variant.template
                    tooltip.add(
                        elements.fluid(
                            JadeFluidObject.of(template.fluid, resource.amount, template.componentsPatch),
                        ),
                    )
                }
                else -> Unit
            }
            tooltip.append(
                Component.translatable(
                    "jade.lazy.replicator.resource",
                    resourceName(resource),
                    resource.amount,
                ),
            )
        }
        tooltip.add(Component.translatable("jade.lazy.replicator.generation_interval", data.intervalTicks))
    }

    @Suppress("UNCHECKED_CAST")
    private fun resourceName(amount: ResourceAmount<out ResourceVariant>): Component {
        val kind = amount.kind as ResourceKind<ResourceVariant>
        return kind.variantName(amount.variant)
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
