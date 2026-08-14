package rhx.lazy.core.io

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties

internal enum class IoMode {
    PASSIVE,
    FACE,
    NETWORK,
}

internal enum class SideIoMode {
    NONE,
    INPUT,
    OUTPUT,
    BOTH,
    ;

    val allowsInput: Boolean
        get() = this == INPUT || this == BOTH

    val allowsOutput: Boolean
        get() = this == OUTPUT || this == BOTH

    /** Machines that never accept an inbound transfer skip the input-capable states while cycling. */
    fun next(allowInput: Boolean = true): SideIoMode {
        var candidate = entries[(ordinal + 1) % entries.size]
        while (!allowInput && candidate.allowsInput) {
            candidate = entries[(candidate.ordinal + 1) % entries.size]
        }
        return candidate
    }
}

internal enum class RelativeSide {
    TOP,
    LEFT,
    FRONT,
    RIGHT,
    BOTTOM,
    BACK,
    ;

    fun toWorldDirection(state: BlockState): Direction =
        when (this) {
            TOP -> Direction.UP
            BOTTOM -> Direction.DOWN
            FRONT -> frontOf(state)
            BACK -> frontOf(state).opposite
            LEFT -> frontOf(state).counterClockWise
            RIGHT -> frontOf(state).clockWise
        }

    companion object {
        fun fromWorldDirection(
            state: BlockState,
            direction: Direction,
        ): RelativeSide {
            if (direction == Direction.UP) return TOP
            if (direction == Direction.DOWN) return BOTTOM
            val front = frontOf(state)
            return when (direction) {
                front -> FRONT
                front.opposite -> BACK
                front.counterClockWise -> LEFT
                else -> RIGHT
            }
        }

        private fun frontOf(state: BlockState): Direction =
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state.getValue(BlockStateProperties.HORIZONTAL_FACING)
            } else {
                Direction.NORTH
            }
    }
}

/**
 * Immutable IO settings shared by every machine and by the configuration card.
 *
 * Instances are treated as values: [networkTarget] and its opaque payload must never be mutated in
 * place. Deep copies are taken only where a tag crosses in or out of the model ([save], [load],
 * [IoController.setNetworkTarget]).
 */
internal data class IoConfiguration(
    val mode: IoMode = IoMode.PASSIVE,
    val sides: Map<RelativeSide, SideIoMode> = DEFAULT_SIDES,
    val autoEject: Boolean = false,
    val networkTarget: NetworkTargetRef? = null,
) {
    val isDefault: Boolean
        get() = this == DEFAULT

    fun side(side: RelativeSide): SideIoMode = sides[side] ?: SideIoMode.NONE

    fun withSide(
        side: RelativeSide,
        sideMode: SideIoMode,
    ): IoConfiguration = copy(sides = sides + (side to sideMode))

    fun deepCopy(): IoConfiguration = copy(networkTarget = networkTarget?.deepCopy())

    fun save(): CompoundTag =
        CompoundTag().apply {
            putString(MODE_TAG, mode.name)
            put(
                SIDES_TAG,
                ListTag().apply {
                    RelativeSide.entries.forEach { side -> add(StringTag.valueOf(this@IoConfiguration.side(side).name)) }
                },
            )
            putBoolean(AUTO_EJECT_TAG, autoEject)
            networkTarget?.let { target -> put(NETWORK_TARGET_TAG, target.save()) }
        }

    companion object {
        private val DEFAULT_SIDES = RelativeSide.entries.associateWith { SideIoMode.NONE }
        val DEFAULT = IoConfiguration()

        fun load(tag: CompoundTag): IoConfiguration {
            val mode = tag.getString(MODE_TAG).toEnumOrDefault(IoMode.PASSIVE)
            val sideList =
                if (tag.contains(SIDES_TAG, Tag.TAG_LIST.toInt())) {
                    tag.getList(SIDES_TAG, Tag.TAG_STRING.toInt())
                } else {
                    ListTag()
                }
            val sides =
                RelativeSide.entries.associateWith { side ->
                    sideList
                        .takeIf { side.ordinal < it.size }
                        ?.getString(side.ordinal)
                        .orEmpty()
                        .toEnumOrDefault(SideIoMode.NONE)
                }
            val target =
                if (tag.contains(NETWORK_TARGET_TAG, Tag.TAG_COMPOUND.toInt())) {
                    NetworkTargetRef.load(tag.getCompound(NETWORK_TARGET_TAG))
                } else {
                    null
                }
            return IoConfiguration(mode, sides, tag.getBoolean(AUTO_EJECT_TAG), target)
        }

        private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T =
            enumValues<T>().firstOrNull { it.name == this } ?: default

        private const val MODE_TAG = "mode"
        private const val SIDES_TAG = "sides"
        private const val AUTO_EJECT_TAG = "auto_eject"
        private const val NETWORK_TARGET_TAG = "network_target"
    }
}
