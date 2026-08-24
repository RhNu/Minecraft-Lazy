package rhx.lazy.feature.simulation

import com.mojang.serialization.JsonOps
import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.neoforged.neoforge.network.connection.ConnectionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationRecipeCodecTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `unprofiled entity uses a valid synthetic recipe id`() {
        val entityId = ResourceLocation.withDefaultNamespace("cow")
        val simulation = ResolvedSimulation.EntityProfile(entityId, null, 1200)

        assertEquals(ResourceLocation.fromNamespaceAndPath("lazy", "entity/minecraft/cow"), simulation.id)
    }

    @Test
    fun `item output codec round trips ranges`() {
        val output = SimulationItemOutput(ItemStack(Items.DIAMOND, 2), 0.25f, 3, 7)
        val encoded =
            SimulationItemOutput.CODEC
                .codec()
                .encodeStart(JsonOps.INSTANCE, output)
                .result()
                .orElseThrow()
        val decoded =
            SimulationItemOutput.CODEC
                .codec()
                .parse(JsonOps.INSTANCE, encoded)
                .result()
                .orElseThrow()

        assertEquals(2, decoded.stack.count)
        assertEquals(0.25f, decoded.chance)
        assertEquals(3..7, decoded.minRolls..decoded.maxRolls)
    }

    @Test
    fun `invalid output probability and range are rejected`() {
        assertTrue(runCatching { SimulationItemOutput(ItemStack(Items.DIAMOND), 1.1f) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(
            runCatching {
                SimulationItemOutput(ItemStack(Items.DIAMOND), minRolls = 4, maxRolls = 3)
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }

    @Test
    fun `batch snapshots recipe outputs instead of retaining the recipe holder`() {
        val original = SimulationItemOutput(ItemStack(Items.DIAMOND), maxRolls = 2)
        val simulation =
            ResolvedSimulation.Item(
                ResourceLocation.fromNamespaceAndPath("lazy_test", "snapshot"),
                1200,
                listOf(original),
                emptyList(),
            )

        val batch = SimulationBatch.from(simulation, 4) as SimulationBatch.Item
        original.stack.count = 42

        assertEquals(
            1,
            batch.itemOutputs
                .single()
                .stack.count,
        )
        assertEquals(4, batch.remaining)
    }

    @Test
    fun `entity profiles reject more outputs than the JEI grid can display`() {
        val displays = List(MAX_OUTPUT_ENTRIES) { ItemStack(Items.DIAMOND) }

        assertTrue(
            runCatching {
                EntitySimulationRecipe(
                    ResourceLocation.withDefaultNamespace("cow"),
                    itemOutputs = listOf(SimulationItemOutput(ItemStack(Items.EMERALD))),
                    displayItemOutputs = displays,
                )
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }

    @Test
    fun `item batch persists block loot descriptions`() {
        val original =
            SimulationBatch.Item(
                emptyList(),
                emptyList(),
                listOf(
                    SimulationBlockLootOutput(
                        Blocks.WHEAT.defaultBlockState(),
                        listOf(ItemStack(Items.WHEAT)),
                        ItemStack(Items.SHEARS),
                    ),
                ),
                3,
            )

        val restored = requireNotNull(SimulationBatch.parse(registries, original.save(registries))) as SimulationBatch.Item

        assertEquals(
            Blocks.WHEAT,
            restored.blockLootOutputs
                .single()
                .state.block,
        )
        assertEquals(
            Items.WHEAT,
            restored.blockLootOutputs
                .single()
                .displayItems
                .single()
                .item,
        )
        assertEquals(
            Items.SHEARS,
            restored.blockLootOutputs
                .single()
                .tool.item,
        )
        assertEquals(3, restored.remaining)
    }

    @Test
    fun `automatic snapshot stream codec preserves runtime and display outputs`() {
        val payload =
            AutomaticSimulationSnapshotPayload(
                listOf(
                    AutomaticSimulationDisplay(
                        ItemStack(Items.WHEAT_SEEDS),
                        ResolvedSimulation.Item(
                            ResourceLocation.fromNamespaceAndPath("lazy", "automatic/crop/minecraft/wheat"),
                            600,
                            listOf(SimulationItemOutput(ItemStack(Items.DIAMOND), 0.25f, 2, 4)),
                            emptyList(),
                            listOf(
                                SimulationBlockLootOutput(
                                    Blocks.WHEAT.defaultBlockState(),
                                    listOf(ItemStack(Items.WHEAT), ItemStack(Items.WHEAT_SEEDS)),
                                    ItemStack(Items.SHEARS),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE)

        try {
            AutomaticSimulationSnapshotPayload.STREAM_CODEC.encode(buffer, payload)
            val decoded = AutomaticSimulationSnapshotPayload.STREAM_CODEC.decode(buffer)
            val display = decoded.displays.single()
            assertEquals(Items.WHEAT_SEEDS, display.input.item)
            assertEquals(600, display.simulation.duration)
            assertEquals(
                0.25f,
                display.simulation.itemOutputs
                    .single()
                    .chance,
            )
            assertEquals(
                listOf(Items.WHEAT, Items.WHEAT_SEEDS),
                display.simulation.blockLootOutputs
                    .single()
                    .displayItems
                    .map { it.item },
            )
            assertEquals(
                Items.SHEARS,
                display.simulation.blockLootOutputs
                    .single()
                    .tool.item,
            )
        } finally {
            buffer.release()
        }
    }
}
