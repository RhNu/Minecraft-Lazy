package rhx.lazy.registry;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

final class BlockEntityTypeBridge {
    private BlockEntityTypeBridge() {}

    static <T extends BlockEntity> BlockEntityType<T> build(BlockEntityType.Builder<T> builder) {
        return builder.build(null);
    }
}
