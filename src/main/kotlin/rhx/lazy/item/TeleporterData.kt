package rhx.lazy.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.Optional

internal data class TeleporterData(
    val returnLocation: SavedLocation?,
    val targetLocation: SavedLocation?,
) {
    companion object {
        val EMPTY = TeleporterData(null, null)

        val CODEC: Codec<TeleporterData> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        SavedLocation.CODEC
                            .optionalFieldOf("return")
                            .forGetter { Optional.ofNullable(it.returnLocation) },
                        SavedLocation.CODEC
                            .optionalFieldOf("target")
                            .forGetter { Optional.ofNullable(it.targetLocation) },
                    ).apply(builder) { returnLocation, targetLocation ->
                        TeleporterData(returnLocation.orElse(null), targetLocation.orElse(null))
                    }
            }
    }
}
