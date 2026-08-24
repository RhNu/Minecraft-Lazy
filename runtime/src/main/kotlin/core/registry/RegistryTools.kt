package rhx.lazy.core.registry

import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.integration.api.LazyInternalApi
import java.util.function.Supplier

@LazyInternalApi
public fun <B : Block, I : BlockItem> DeferredRegister.Items.registerBlockItem(
    name: String,
    block: DeferredBlock<B>,
    factory: (B, Item.Properties) -> I,
): DeferredItem<I> =
    register(
        name,
        Supplier { factory(block.get(), Item.Properties()) },
    )

@LazyInternalApi
public fun <T : BlockEntity> BlockEntityType.Builder<T>.buildType(): BlockEntityType<T> = BlockEntityTypeBridge.build(this)
