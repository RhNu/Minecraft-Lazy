package rhx.lazy.feature.teleporter

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

internal object TeleporterDimensionBlacklist {
    private val blacklistedDimensions =
        setOf(
            ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("compactmachines", "compact_world"),
            ),
        )

    fun contains(dimension: ResourceKey<Level>): Boolean = dimension in blacklistedDimensions
}
