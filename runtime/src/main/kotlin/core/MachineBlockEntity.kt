package rhx.lazy.core

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import rhx.lazy.core.render.MachineDisplayState
import rhx.lazy.integration.api.LazyInternalApi

/**
 * What a machine leaves behind when it is removed, and what it shows while it is standing there.
 *
 * Storage travels with the machine: a buffer keeps its stacks, a chamber keeps its batch. Settings
 * never do, so a machine that is placed again always starts from its defaults — [settingKeys] names
 * the persisted keys that are stripped on the way out. [MachineBlock] owns the ordering and the
 * delivery; machines only describe what they hold.
 *
 * A machine that wants to be recognisable from the outside overrides [computeDisplayState]. That is
 * the only opt-in: machines that keep the default never compute, store or send anything, and their
 * update packet stays null, so the world-render channel costs nothing until a machine uses it.
 */
@LazyInternalApi
public abstract class MachineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : ManagedBlockEntity(type, pos, state) {
    /** Spreads a wall of machines across the poll cycle instead of landing every check on one tick. */
    private val displayPhase = pos.hashCode().mod(DISPLAY_POLL_INTERVAL)
    private var displayState: MachineDisplayState? = null
    private var nextActivityPublish = 0L

    /** True when public storage has to ride along on the dropped machine item. */
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

    /**
     * What this machine shows in the world, or null when it shows nothing at all. Server side only.
     *
     * Implementations must stay cheap: this runs on every poll, and once more whenever a chunk is
     * sent to a player.
     */
    protected open fun computeDisplayState(): MachineDisplayState? = null

    /** The state a renderer draws. The server computes it, the client receives it. */
    fun displayState(): MachineDisplayState? = displayState

    /**
     * Publishes the display state when it changed.
     *
     * An icon change goes out at once, because a player just caused it and expects to see it. An
     * activity flip is rate limited instead: an output backlog draining against a hopper can toggle
     * a machine between running and blocked far faster than an eye can follow, and there is no point
     * paying for updates nobody can read. Only a flip spends the budget, so a machine that lights up
     * on the tick after a player loaded it still lights up immediately.
     */
    protected fun refreshDisplayState() {
        val level = level as? ServerLevel ?: return
        val next = computeDisplayState() ?: return
        val current = displayState
        val iconChanged = current == null || !ItemStack.matches(current.icon, next.icon)
        val activityChanged = current == null || current.activity != next.activity
        if (!iconChanged && !activityChanged) return
        if (!iconChanged && level.gameTime < nextActivityPublish) return
        displayState = next
        if (activityChanged) nextActivityPublish = level.gameTime + ACTIVITY_SYNC_INTERVAL
        level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
    }

    /**
     * One poll, on this machine's own slot of the cycle. Machines call it once per server tick; a
     * whole wall of them still spreads its checks evenly over [DISPLAY_POLL_INTERVAL] ticks.
     */
    protected fun tickDisplayState() {
        val level = level ?: return
        if ((level.gameTime + displayPhase) % DISPLAY_POLL_INTERVAL != 0L) return
        refreshDisplayState()
    }

    /**
     * Recomputes on the server so a chunk sent before this machine's first poll already carries the
     * right state, and a player never watches icons pop in behind them.
     */
    private fun currentDisplayState(): MachineDisplayState? {
        if (level?.isClientSide != false) return displayState
        return computeDisplayState()?.also { displayState = it }
    }

    final override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val state = currentDisplayState() ?: return CompoundTag()
        return CompoundTag().apply { put(DISPLAY_TAG, state.save(registries)) }
    }

    final override fun getUpdatePacket(): Packet<ClientGamePacketListener>? =
        if (currentDisplayState() == null) null else ClientboundBlockEntityDataPacket.create(this)

    /**
     * Reads only the display state. The update tag never carries machine data, so the default — a
     * full load — would hand the client an empty machine.
     */
    final override fun handleUpdateTag(
        tag: CompoundTag,
        lookupProvider: HolderLookup.Provider,
    ) {
        if (!tag.contains(DISPLAY_TAG, Tag.TAG_COMPOUND.toInt())) return
        displayState = MachineDisplayState.parse(lookupProvider, tag.getCompound(DISPLAY_TAG))
    }

    final override fun onDataPacket(
        connection: Connection,
        packet: ClientboundBlockEntityDataPacket,
        lookupProvider: HolderLookup.Provider,
    ) {
        handleUpdateTag(packet.tag, lookupProvider)
    }

    private companion object {
        const val DISPLAY_TAG = "lazyDisplay"

        /** Ticks between polls. One second of latency on a lamp nobody is staring at is free. */
        const val DISPLAY_POLL_INTERVAL = 20

        /** Shortest gap between two activity-only updates for the same machine. */
        const val ACTIVITY_SYNC_INTERVAL = 20L
    }
}
