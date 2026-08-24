package rhx.lazy.core

import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public inline fun <reified E : BlockEntity, T : BlockEntity?> Level.serverTicker(
    actualType: BlockEntityType<T>,
    expectedType: BlockEntityType<out E>,
    crossinline tick: E.() -> Unit,
): BlockEntityTicker<T>? =
    if (isClientSide || actualType != expectedType) {
        null
    } else {
        BlockEntityTicker { _, _, _, entity -> (entity as? E)?.tick() }
    }
