package rhx.lazy.feature.voidworld

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.LevelStem
import rhx.lazy.core.lazyId

internal object VoidWorldKeys {
    val voidBiome: ResourceKey<Biome> = ResourceKey.create(Registries.BIOME, lazyId("void"))
    val voidDimensionType: ResourceKey<DimensionType> =
        ResourceKey.create(Registries.DIMENSION_TYPE, lazyId("void"))
    val voidLevel: ResourceKey<Level> = ResourceKey.create(Registries.DIMENSION, lazyId("void"))
    val voidLevelStem: ResourceKey<LevelStem> =
        ResourceKey.create(Registries.LEVEL_STEM, lazyId("void"))
}
