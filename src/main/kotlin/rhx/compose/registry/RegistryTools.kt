package rhx.compose.registry

import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredItem
import java.util.function.Supplier

internal data class RegisteredBlock<T : Block>(
    val block: DeferredBlock<T>,
    val item: DeferredItem<BlockItem>,
)

internal fun <T : Block> registerBlockWithItem(
    name: String,
    blockFactory: Supplier<T>,
    itemProperties: Supplier<Item.Properties> = Supplier(::defaultItemProperties),
): RegisteredBlock<T> {
    val block = ModBlocks.registry.register(name, blockFactory)
    val item =
        ModItems.registry.register(
            name,
            Supplier { BlockItem(block.get(), itemProperties.get()) },
        )
    return RegisteredBlock(block, item)
}

private fun defaultItemProperties(): Item.Properties = Item.Properties()

internal fun <T : BlockEntity> BlockEntityType.Builder<T>.buildType(): BlockEntityType<T> =
    BlockEntityTypeBridge.build(this)
