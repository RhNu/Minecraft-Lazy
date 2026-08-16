package rhx.lazy.core

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * What a machine leaves behind when it is removed.
 *
 * Storage travels with the machine: a buffer keeps its stacks, a chamber keeps its batch. Settings
 * never do, so a machine that is placed again always starts from its defaults — [settingKeys] names
 * the persisted keys that are stripped on the way out. [MachineBlock] owns the ordering and the
 * delivery; machines only describe what they hold.
 */
internal abstract class MachineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : ManagedBlockEntity(type, pos, state) {
    /** True when internal storage has to ride along on the dropped machine item. */
    open fun hasStoredContents(): Boolean = false

    /** Items the machine only held for a player. They drop beside it, and leave the machine empty. */
    open fun takeHeldItems(): List<ItemStack> = emptyList()

    /** Persisted keys that describe settings rather than storage. */
    protected open fun settingKeys(): Set<String> = emptySet()

    /**
     * Writes this machine's storage onto [stack]. A machine whose data ends up empty keeps no
     * block-entity component at all, so untouched machines still stack with fresh ones.
     */
    fun saveContentsToItem(
        stack: ItemStack,
        registries: HolderLookup.Provider,
    ) {
        saveToItem(stack, registries)
        val keys = settingKeys()
        if (keys.isEmpty()) return
        val tag = stack.get(DataComponents.BLOCK_ENTITY_DATA)?.copyTag() ?: return
        if (keys.none(tag::contains)) return
        keys.forEach(tag::remove)
        BlockItem.setBlockEntityData(stack, type, tag)
    }
}
