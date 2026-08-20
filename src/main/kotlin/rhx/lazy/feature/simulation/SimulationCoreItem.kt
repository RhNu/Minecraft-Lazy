package rhx.lazy.feature.simulation

import net.minecraft.world.item.Item

internal class SimulationCoreItem(
    val tier: SimulationCoreTier,
    properties: Properties,
) : Item(properties)
