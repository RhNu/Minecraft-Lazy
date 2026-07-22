package rhx.lazy.item

import com.mojang.serialization.JsonOps
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeleporterDataTest {
    @Test
    fun `location and optional endpoints survive codec round trip`() {
        val returnLocation = SavedLocation(Level.OVERWORLD, BlockPos(12, 64, -8), 90.0f, -15.0f)
        val value = TeleporterData(returnLocation, null)
        val encoded = TeleporterData.CODEC.encodeStart(JsonOps.INSTANCE, value).result().orElseThrow()
        val decoded = TeleporterData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow()

        assertEquals(returnLocation, decoded.returnLocation)
        assertNull(decoded.targetLocation)
    }

    @Test
    fun `routing uses defaults and commits only the endpoint that changed`() {
        val outside = SavedLocation(Level.OVERWORLD, BlockPos(1, 65, 1), 0.0f, 0.0f)
        val voidCurrent = SavedLocation(Level.END, BlockPos(5, 129, 5), 10.0f, 5.0f)
        val defaultReturn = SavedLocation(Level.OVERWORLD, BlockPos.ZERO, 0.0f, 0.0f)
        val defaultTarget = SavedLocation(Level.END, BlockPos(0, 129, 0), 0.0f, 0.0f)

        val entering =
            TeleporterRouting.select(false, outside, TeleporterData.EMPTY, defaultReturn, defaultTarget)
        assertEquals(defaultTarget, entering.requestedDestination)
        assertEquals(TeleporterData(outside, defaultTarget), entering.completed(defaultTarget))

        val returningData = TeleporterData(outside, defaultTarget)
        val leaving =
            TeleporterRouting.select(true, voidCurrent, returningData, defaultReturn, defaultTarget)
        assertEquals(outside, leaving.requestedDestination)
        assertEquals(returningData.copy(targetLocation = voidCurrent), leaving.completed(outside))

        val leavingWithoutReturn =
            TeleporterRouting.select(true, voidCurrent, TeleporterData.EMPTY, defaultReturn, defaultTarget)
        assertEquals(defaultReturn, leavingWithoutReturn.requestedDestination)
        assertEquals(TeleporterData(null, voidCurrent), leavingWithoutReturn.completed(defaultReturn))
    }

    @Test
    fun `codec preserves a syntactically valid missing dimension and rejects an invalid id`() {
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
        val encoded = SavedLocation.CODEC.encodeStart(JsonOps.INSTANCE, missingDimension).result().orElseThrow()
        assertEquals(
            missingDimension,
            SavedLocation.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow(),
        )

        val invalid =
            com.google.gson.JsonParser.parseString(
                """{"dimension":"lazy:Invalid Dimension","pos":[0,0,0],"yaw":0,"pitch":0}""",
            )
        assertTrue(SavedLocation.CODEC.parse(JsonOps.INSTANCE, invalid).isError)
    }
}
