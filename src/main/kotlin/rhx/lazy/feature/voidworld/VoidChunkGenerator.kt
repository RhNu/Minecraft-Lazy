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
import net.minecraft.world.level.block.state.BlockState
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
        internal const val PLATFORM_Y = 64
        private const val PLATFORM_RADIUS = 2

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

        internal fun isPlatformCoordinate(
            worldX: Int,
            worldZ: Int,
        ): Boolean =
            worldX in -PLATFORM_RADIUS..PLATFORM_RADIUS &&
                worldZ in -PLATFORM_RADIUS..PLATFORM_RADIUS

        internal fun platformStateAt(
            worldX: Int,
            worldZ: Int,
        ): BlockState? =
            if (!isPlatformCoordinate(worldX, worldZ)) {
                null
            } else if (kotlin.math.abs(worldX) == PLATFORM_RADIUS || kotlin.math.abs(worldZ) == PLATFORM_RADIUS) {
                Blocks.STONE_BRICKS.defaultBlockState()
            } else {
                Blocks.SMOOTH_STONE.defaultBlockState()
            }

        internal fun baseHeightFor(
            state: BlockState,
            type: Heightmap.Types,
            minBuildHeight: Int,
            platformY: Int,
        ): Int =
            if (type.isOpaque().test(state)) {
                platformY + 1
            } else {
                minBuildHeight
            }
    }

    private val airState = Blocks.AIR.defaultBlockState()

    override fun codec(): MapCodec<out ChunkGenerator> = MAP_CODEC

    override fun getGenDepth(): Int = GENERATION_DEPTH

    override fun getSeaLevel(): Int = MIN_Y

    override fun getMinY(): Int = MIN_Y

    override fun getSpawnHeight(level: LevelHeightAccessor): Int = PLATFORM_Y + 1

    override fun fillFromNoise(
        blender: Blender,
        randomState: RandomState,
        structureManager: StructureManager,
        chunk: ChunkAccess,
    ): CompletableFuture<ChunkAccess> {
        val mutablePos = BlockPos.MutableBlockPos()
        val oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG)
        val worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)

        for (x in 0 until 16) {
            for (z in 0 until 16) {
                val worldX = chunk.pos.minBlockX + x
                val worldZ = chunk.pos.minBlockZ + z
                val state = platformStateAt(worldX, worldZ) ?: airState
                chunk.setBlockState(mutablePos.set(x, PLATFORM_Y, z), state, false)
                oceanFloor.update(x, PLATFORM_Y, z, state)
                worldSurface.update(x, PLATFORM_Y, z, state)
            }
        }

        return CompletableFuture.completedFuture(chunk)
    }

    override fun getBaseHeight(
        x: Int,
        z: Int,
        type: Heightmap.Types,
        level: LevelHeightAccessor,
        randomState: RandomState,
    ): Int =
        baseHeightFor(
            state = platformStateAt(x, z) ?: airState,
            type = type,
            minBuildHeight = level.minBuildHeight,
            platformY = PLATFORM_Y,
        )

    override fun getBaseColumn(
        x: Int,
        z: Int,
        level: LevelHeightAccessor,
        randomState: RandomState,
    ): NoiseColumn {
        val states = Array(level.height) { airState }
        val stateIndex = PLATFORM_Y - level.minBuildHeight
        if (stateIndex in states.indices) {
            states[stateIndex] = platformStateAt(x, z) ?: airState
        }
        return NoiseColumn(level.minBuildHeight, states)
    }

    override fun addDebugScreenInfo(
        info: MutableList<String>,
        randomState: RandomState,
        pos: BlockPos,
    ) {
        info.add("Lazy void generator; platform at Y=$PLATFORM_Y")
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
