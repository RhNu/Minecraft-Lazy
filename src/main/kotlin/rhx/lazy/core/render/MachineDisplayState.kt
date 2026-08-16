package rhx.lazy.core.render

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * How alive a machine looks from the outside.
 *
 * The three values are what a player can tell apart at a glance across a wall of machines, and no
 * more: a running machine is lit, an idle one is not, and a blocked one is lit and flagged. Anything
 * finer — why it is blocked, how far along it is — belongs in the machine's screen or its Jade view.
 */
internal enum class MachineActivity {
    IDLE,
    RUNNING,
    BLOCKED,
}

/**
 * What a machine shows in the world, and the whole of what it sends to clients for rendering.
 *
 * The icon is already resolved: machines publish the stack they want drawn rather than the raw
 * contents of a slot, so a target that is not self-identifying — a data model bound to an entity —
 * turns into something recognisable before it leaves the server. Renderers stay policy-free.
 */
internal class MachineDisplayState(
    val icon: ItemStack,
    val activity: MachineActivity,
) {
    val isEmpty: Boolean
        get() = icon.isEmpty

    /**
     * Value comparison. [ItemStack] does not implement `equals`, so this is the only correct way to
     * ask whether two states would render the same.
     */
    fun matches(other: MachineDisplayState): Boolean = activity == other.activity && ItemStack.matches(icon, other.icon)

    fun save(registries: HolderLookup.Provider): CompoundTag =
        CompoundTag().apply {
            putByte(ACTIVITY_KEY, activity.ordinal.toByte())
            if (!icon.isEmpty) put(ICON_KEY, icon.save(registries))
        }

    companion object {
        /** A machine that renders nothing right now but still takes part in display sync. */
        val EMPTY = MachineDisplayState(ItemStack.EMPTY, MachineActivity.IDLE)

        private const val ICON_KEY = "icon"
        private const val ACTIVITY_KEY = "activity"

        fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): MachineDisplayState =
            MachineDisplayState(
                ItemStack.parseOptional(registries, tag.getCompound(ICON_KEY)),
                MachineActivity.entries.getOrElse(tag.getByte(ACTIVITY_KEY).toInt()) { MachineActivity.IDLE },
            )
    }
}
