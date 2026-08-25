package rhx.lazy.feature.teleporter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import java.util.Optional
import java.util.UUID

internal data class TeleporterPlayerState(
    val externalReturn: SavedLocation?,
    val selectedSpaceId: UUID?,
) {
    companion object {
        val EMPTY = TeleporterPlayerState(null, null)

        val CODEC: Codec<TeleporterPlayerState> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        SavedLocation.CODEC
                            .optionalFieldOf("external_return")
                            .forGetter { state -> Optional.ofNullable(state.externalReturn) },
                        UUIDUtil.CODEC
                            .optionalFieldOf("selected_space")
                            .forGetter { state -> Optional.ofNullable(state.selectedSpaceId) },
                    ).apply(builder) { externalReturn, selectedSpaceId ->
                        TeleporterPlayerState(
                            externalReturn = externalReturn.orElse(null),
                            selectedSpaceId = selectedSpaceId.orElse(null),
                        )
                    }
            }
    }
}
