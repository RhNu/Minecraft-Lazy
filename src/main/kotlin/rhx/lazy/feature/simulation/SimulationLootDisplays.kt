package rhx.lazy.feature.simulation

import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import java.util.WeakHashMap

/**
 * Context-aware loot candidates shown in previews. Fixed probe seeds keep discovery deterministic,
 * do not consume the world's random sequence, and let modded loot conditions/functions run through
 * the same loaded table used by production.
 */
internal object SimulationLootDisplays {
    private val caches = WeakHashMap<MinecraftServer, MutableMap<PreviewKey, List<ItemStack>>>()

    fun items(
        level: Level,
        state: BlockState,
        tool: ItemStack = ItemStack.EMPTY,
    ): List<ItemStack> {
        val serverLevel = level as? ServerLevel ?: return fallback(state)
        val key = PreviewKey(state, tool.item, tool.componentsPatch)
        val cache = synchronized(this) { caches.getOrPut(serverLevel.server, ::hashMapOf) }
        synchronized(cache) { cache[key]?.let { return it.map(ItemStack::copy) } }

        val discovered = sample(serverLevel, state, tool)
        synchronized(cache) { cache[key] = discovered.map(ItemStack::copy) }
        return discovered
    }

    @Synchronized
    fun invalidate() {
        caches.clear()
    }

    private fun sample(
        level: ServerLevel,
        state: BlockState,
        tool: ItemStack,
    ): List<ItemStack> {
        val table = level.server.reloadableRegistries().getLootTable(state.block.lootTable)
        val params =
            LootParams
                .Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.ZERO))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .create(LootContextParamSets.BLOCK)
        val found = mutableListOf<ItemStack>()
        repeat(PREVIEW_PROBES) { probe ->
            val seed = PROBE_STEP * (probe + 1L)
            table
                .getRandomItems(params, seed)
                .asSequence()
                .filterNot(ItemStack::isEmpty)
                .forEach { stack ->
                    if (found.none { ItemStack.isSameItemSameComponents(it, stack) }) {
                        found += stack.copyWithCount(1)
                    }
                }
        }
        return found
    }

    private fun fallback(state: BlockState): List<ItemStack> =
        state.block
            .asItem()
            .takeUnless { it === Items.AIR }
            ?.let { listOf(ItemStack(it)) }
            .orEmpty()

    private data class PreviewKey(
        val state: BlockState,
        val tool: Item,
        val components: DataComponentPatch,
    )

    private const val PREVIEW_PROBES = 128
    private const val PROBE_STEP = -7046029254386353131L
}

internal fun blockLoot(
    level: Level,
    state: BlockState,
    tool: ItemStack = ItemStack.EMPTY,
) = SimulationBlockLootOutput(
    state,
    SimulationLootDisplays.items(level, state, tool),
    tool,
)
