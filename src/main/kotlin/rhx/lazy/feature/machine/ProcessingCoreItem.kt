package rhx.lazy.feature.machine

import net.minecraft.world.item.Item

internal class ProcessingCoreItem(
    val tier: ProcessingCoreTier,
    properties: Properties,
) : Item(properties)
