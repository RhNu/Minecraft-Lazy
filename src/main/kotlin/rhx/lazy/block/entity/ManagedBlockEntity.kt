package rhx.lazy.block.entity

import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IBlockEntityManaged
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IBlockEntityManagedHolder
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

internal abstract class ManagedBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(type, pos, state),
    IPersistManagedHolder,
    IBlockEntityManaged,
    IBlockEntityManagedHolder {
    private val managedStorage = FieldManagedStorage(this)

    override fun getSyncStorage(): IManagedStorage = managedStorage

    override fun getRootStorage(): IManagedStorage = managedStorage
}
