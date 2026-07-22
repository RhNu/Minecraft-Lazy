package rhx.lazy.util

import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType

internal inline fun <reified E : BlockEntity, T : BlockEntity?> Level.serverTicker(
    actualType: BlockEntityType<T>,
    expectedType: BlockEntityType<out E>,
    crossinline tick: E.() -> Unit,
): BlockEntityTicker<T>? =
    if (isClientSide || actualType != expectedType) {
        null
    } else {
        BlockEntityTicker { _, _, _, entity -> (entity as? E)?.tick() }
    }
