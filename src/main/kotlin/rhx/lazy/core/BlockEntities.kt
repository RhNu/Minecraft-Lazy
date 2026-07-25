package rhx.lazy.core

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

internal fun <E : BlockEntity> BlockGetter.blockEntityOrNull(
    pos: BlockPos,
    type: BlockEntityType<E>,
): E? = getBlockEntity(pos, type).orElse(null)
