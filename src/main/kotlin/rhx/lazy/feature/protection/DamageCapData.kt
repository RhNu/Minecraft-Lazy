package rhx.lazy.feature.protection

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

internal data class DamageCapData(
    val enabled: Boolean = false,
    val threshold: Int = 0,
) {
    init {
        require(threshold >= 0) { "Damage cap threshold must not be negative" }
    }

    companion object {
        val CODEC: Codec<DamageCapData> =
            RecordCodecBuilder.create { instance ->
                instance
                    .group(
                        Codec.BOOL.fieldOf("enabled").forGetter(DamageCapData::enabled),
                        Codec.intRange(0, Int.MAX_VALUE).fieldOf("threshold").forGetter(DamageCapData::threshold),
                    ).apply(instance, ::DamageCapData)
            }
    }
}
