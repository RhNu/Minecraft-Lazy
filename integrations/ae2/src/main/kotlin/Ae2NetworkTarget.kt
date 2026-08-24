package rhx.lazy.integration.ae2

import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import rhx.lazy.core.io.NetworkTargetRef

internal object Ae2NetworkTarget {
    private const val DIMENSION_TAG = "dimension"
    private const val POSITION_TAG = "position"

    fun create(pos: GlobalPos): NetworkTargetRef =
        NetworkTargetRef(
            Ae2NetworkOutputProvider.ID,
            CompoundTag().apply {
                putString(DIMENSION_TAG, pos.dimension.location().toString())
                putLong(POSITION_TAG, pos.pos.asLong())
            },
        )

    fun parse(target: NetworkTargetRef): GlobalPos? {
        if (target.providerId != Ae2NetworkOutputProvider.ID) return null
        if (!target.data.contains(DIMENSION_TAG, Tag.TAG_STRING.toInt())) return null
        if (!target.data.contains(POSITION_TAG, Tag.TAG_LONG.toInt())) return null
        val dimensionId = ResourceLocation.tryParse(target.data.getString(DIMENSION_TAG)) ?: return null
        return GlobalPos.of(
            ResourceKey.create(Registries.DIMENSION, dimensionId),
            BlockPos.of(target.data.getLong(POSITION_TAG)),
        )
    }
}
