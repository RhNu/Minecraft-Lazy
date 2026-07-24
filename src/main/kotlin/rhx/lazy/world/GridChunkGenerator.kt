package rhx.lazy.world

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.NoiseColumn
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.biome.FixedBiomeSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.levelgen.blending.Blender
import java.util.concurrent.CompletableFuture

internal class GridChunkGenerator(
    internal val settings: GridGeneratorSettings,
) : ChunkGenerator(FixedBiomeSource(settings.biome)) {
    companion object {
        private const val MIN_Y = -64
        private const val GENERATION_DEPTH = 384

        val CODEC: Codec<GridChunkGenerator> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        GridGeneratorSettings.CODEC
                            .fieldOf("settings")
                            .forGetter(GridChunkGenerator::settings),
                    ).apply(builder, ::GridChunkGenerator)
            }
        val MAP_CODEC: MapCodec<GridChunkGenerator> = MapCodec.assumeMapUnsafe(CODEC)

        internal fun isBorderCoordinate(
            worldX: Int,
            worldZ: Int,
            gridSize: Int,
        ): Boolean {
            require(gridSize > 0) { "gridSize must be positive" }
            val modX = Math.floorMod(worldX, gridSize)
            val modZ = Math.floorMod(worldZ, gridSize)
            return modX == 0 || modX == gridSize - 1 || modZ == 0 || modZ == gridSize - 1
        }

        internal fun baseHeightFor(
            state: BlockState,
            type: Heightmap.Types,
            minBuildHeight: Int,
            layerHeight: Int,
        ): Int =
            if (type.isOpaque().test(state)) {
                layerHeight + 1
            } else {
                minBuildHeight
            }
    }

    private val gridSize = settings.gridChunkSize * 16
    private val borderState = settings.borderBlock.defaultBlockState()
    private val innerState = settings.innerBlock.defaultBlockState()

    override fun codec(): MapCodec<out ChunkGenerator> = MAP_CODEC

    override fun getGenDepth(): Int = GENERATION_DEPTH

    override fun getSeaLevel(): Int = MIN_Y

    override fun getMinY(): Int = MIN_Y

    override fun fillFromNoise(
        blender: Blender,
        randomState: RandomState,
        structureManager: StructureManager,
        chunk: ChunkAccess,
    ): CompletableFuture<ChunkAccess> {
        val mutablePos = BlockPos.MutableBlockPos()
        val oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG)
        val worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
        val y = settings.layerHeight

        for (x in 0 until 16) {
            for (z in 0 until 16) {
                val worldX = chunk.pos.minBlockX + x
                val worldZ = chunk.pos.minBlockZ + z
                val state = stateAt(worldX, worldZ)
                chunk.setBlockState(mutablePos.set(x, y, z), state, false)
                oceanFloor.update(x, y, z, state)
                worldSurface.update(x, y, z, state)
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
            state = stateAt(x, z),
            type = type,
            minBuildHeight = level.minBuildHeight,
            layerHeight = settings.layerHeight,
        )

    override fun getBaseColumn(
        x: Int,
        z: Int,
        level: LevelHeightAccessor,
        randomState: RandomState,
    ): NoiseColumn {
        val states = Array(level.height) { Blocks.AIR.defaultBlockState() }
        val stateIndex = settings.layerHeight - level.minBuildHeight
        if (stateIndex in states.indices) {
            states[stateIndex] = stateAt(x, z)
        }
        return NoiseColumn(level.minBuildHeight, states)
    }

    private fun stateAt(
        worldX: Int,
        worldZ: Int,
    ): BlockState =
        if (isBorderCoordinate(worldX, worldZ, gridSize)) {
            borderState
        } else {
            innerState
        }

    override fun addDebugScreenInfo(
        info: MutableList<String>,
        randomState: RandomState,
        pos: BlockPos,
    ) {
        info.add("Lazy grid generator at Y=${settings.layerHeight}")
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

internal data class GridGeneratorSettings(
    val layerHeight: Int,
    val gridChunkSize: Int,
    val biome: Holder<Biome>,
    val borderBlock: Block,
    val innerBlock: Block,
) {
    companion object {
        val CODEC: Codec<GridGeneratorSettings> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        Codec
                            .intRange(-64, 317)
                            .fieldOf("layer_height")
                            .orElse(128)
                            .forGetter(GridGeneratorSettings::layerHeight),
                        Codec
                            .intRange(1, 64)
                            .fieldOf("grid_chunk_size")
                            .orElse(3)
                            .forGetter(GridGeneratorSettings::gridChunkSize),
                        Biome.CODEC.fieldOf("biome").forGetter(GridGeneratorSettings::biome),
                        BuiltInRegistries.BLOCK
                            .byNameCodec()
                            .fieldOf("border_block")
                            .orElse(Blocks.STONE_BRICKS)
                            .forGetter(GridGeneratorSettings::borderBlock),
                        BuiltInRegistries.BLOCK
                            .byNameCodec()
                            .fieldOf("inner_block")
                            .orElse(Blocks.SMOOTH_STONE)
                            .forGetter(GridGeneratorSettings::innerBlock),
                    ).apply(builder, ::GridGeneratorSettings)
            }
    }
}
