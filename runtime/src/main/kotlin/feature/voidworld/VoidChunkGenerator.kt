package rhx.lazy.feature.voidworld

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.NoiseColumn
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.biome.FixedBiomeSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.levelgen.blending.Blender
import java.util.concurrent.CompletableFuture

internal class VoidChunkGenerator(
    internal val biome: Holder<Biome>,
) : ChunkGenerator(FixedBiomeSource(biome)) {
    companion object {
        private const val MIN_Y = -64
        private const val GENERATION_DEPTH = 192
        private const val SPAWN_HEIGHT = 65

        val CODEC: Codec<VoidChunkGenerator> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        Biome.CODEC
                            .fieldOf("biome")
                            .forGetter(VoidChunkGenerator::biome),
                    ).apply(builder, ::VoidChunkGenerator)
            }
        val MAP_CODEC: MapCodec<VoidChunkGenerator> = MapCodec.assumeMapUnsafe(CODEC)

        internal fun emptyColumnStates(height: Int) = Array(height) { Blocks.AIR.defaultBlockState() }
    }

    override fun codec(): MapCodec<out ChunkGenerator> = MAP_CODEC

    override fun getGenDepth(): Int = GENERATION_DEPTH

    override fun getSeaLevel(): Int = MIN_Y

    override fun getMinY(): Int = MIN_Y

    override fun getSpawnHeight(level: LevelHeightAccessor): Int = SPAWN_HEIGHT

    override fun fillFromNoise(
        blender: Blender,
        randomState: RandomState,
        structureManager: StructureManager,
        chunk: ChunkAccess,
    ): CompletableFuture<ChunkAccess> = CompletableFuture.completedFuture(chunk)

    override fun getBaseHeight(
        x: Int,
        z: Int,
        type: Heightmap.Types,
        level: LevelHeightAccessor,
        randomState: RandomState,
    ): Int = level.minBuildHeight

    override fun getBaseColumn(
        x: Int,
        z: Int,
        level: LevelHeightAccessor,
        randomState: RandomState,
    ): NoiseColumn =
        NoiseColumn(
            level.minBuildHeight,
            emptyColumnStates(level.height),
        )

    override fun addDebugScreenInfo(
        info: MutableList<String>,
        randomState: RandomState,
        pos: BlockPos,
    ) {
        info.add("Lazy void generator")
    }

    override fun applyCarvers(
        level: WorldGenRegion,
        seed: Long,
        randomState: RandomState,
        biomeManager: BiomeManager,
        structureManager: StructureManager,
        chunk: ChunkAccess,
        step: GenerationStep.Carving,
    ) = Unit

    override fun buildSurface(
        level: WorldGenRegion,
        structureManager: StructureManager,
        randomState: RandomState,
        chunk: ChunkAccess,
    ) = Unit

    override fun spawnOriginalMobs(level: WorldGenRegion) = Unit
}
