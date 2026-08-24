package rhx.lazy.feature.energy

import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

internal class EnergySourceBlockItem(
    block: Block,
    properties: Item.Properties,
) : BlockItem(block, properties)
