package rhx.lazy.feature.teleporter

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import rhx.lazy.core.testing.jsonRoundTrip
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TeleporterPlayerStateTest {
    @Test
    fun `return point and selected space survive codec round trip`() {
        val returnLocation = SavedLocation(Level.OVERWORLD, BlockPos(12, 64, -8), 90.0f, -15.0f)
        val value = TeleporterPlayerState(returnLocation, UUID.fromString("f6a46e4c-39cd-43f5-bb22-f01ee3eb8e20"))

        assertEquals(value, TeleporterPlayerState.CODEC.jsonRoundTrip(value))
        assertEquals(TeleporterPlayerState.EMPTY, TeleporterPlayerState.CODEC.jsonRoundTrip(TeleporterPlayerState.EMPTY))
    }

    @Test
    fun `codec preserves a syntactically valid missing dimension`() {
        val missingDimension =
            SavedLocation(
                ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath("lazy", "missing"),
                ),
                BlockPos.ZERO,
                0.0f,
                0.0f,
            )

        assertEquals(
            missingDimension,
            SavedLocation.CODEC.jsonRoundTrip(missingDimension),
        )
    }
}
