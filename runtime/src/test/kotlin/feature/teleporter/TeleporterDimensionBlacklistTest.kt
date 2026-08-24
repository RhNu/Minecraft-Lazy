package rhx.lazy.feature.teleporter

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeleporterDimensionBlacklistTest {
    @Test
    fun `compact machines dimension is blacklisted`() {
        val compactMachinesDimension =
            ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("compactmachines", "compact_world"),
            )

        assertTrue(TeleporterDimensionBlacklist.contains(compactMachinesDimension))
    }

    @Test
    fun `other dimensions are allowed`() {
        assertFalse(TeleporterDimensionBlacklist.contains(Level.OVERWORLD))
    }
}
