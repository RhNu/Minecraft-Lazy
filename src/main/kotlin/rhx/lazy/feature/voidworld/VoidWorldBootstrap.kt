package rhx.lazy.feature.voidworld

import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.tags.BlockTags
import net.minecraft.util.valueproviders.ConstantInt
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeGenerationSettings
import net.minecraft.world.level.biome.BiomeSpecialEffects
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.LevelStem
import java.util.OptionalLong

internal object VoidWorldBootstrap {
    fun bootstrapBiome(context: BootstrapContext<Biome>) {
        val placedFeatures = context.lookup(Registries.PLACED_FEATURE)
        val carvers = context.lookup(Registries.CONFIGURED_CARVER)
        val biome =
            Biome
                .BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8f)
                .downfall(0.0f)
                .specialEffects(
                    BiomeSpecialEffects
                        .Builder()
                        .fogColor(12_638_463)
                        .skyColor(4_159_204)
                        .waterColor(0xFFFFFF)
                        .waterFogColor(0xFFFFFF)
                        .build(),
                ).mobSpawnSettings(MobSpawnSettings.Builder().build())
                .generationSettings(BiomeGenerationSettings.Builder(placedFeatures, carvers).build())
                .build()
        context.register(VoidWorldKeys.voidBiome, biome)
    }

    fun bootstrapDimensionType(context: BootstrapContext<DimensionType>) {
        context.register(
            VoidWorldKeys.voidDimensionType,
            DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                true,
                1.0,
                true,
                false,
                -64,
                384,
                384,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                0.5f,
                DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0),
            ),
        )
    }

    fun bootstrapLevelStem(context: BootstrapContext<LevelStem>) {
        val biomes = context.lookup(Registries.BIOME)
        val dimensionTypes = context.lookup(Registries.DIMENSION_TYPE)
        val generator =
            GridChunkGenerator(
                GridGeneratorSettings(
                    layerHeight = 128,
                    gridChunkSize = 3,
                    biome = biomes.getOrThrow(VoidWorldKeys.voidBiome),
                    borderBlock = Blocks.STONE_BRICKS,
                    innerBlock = Blocks.SMOOTH_STONE,
                ),
            )
        context.register(
            VoidWorldKeys.voidLevelStem,
            LevelStem(dimensionTypes.getOrThrow(VoidWorldKeys.voidDimensionType), generator),
        )
    }
}
