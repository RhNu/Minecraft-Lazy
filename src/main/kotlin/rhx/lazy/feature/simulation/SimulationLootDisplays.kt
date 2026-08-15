package rhx.lazy.feature.simulation

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootTable
import rhx.lazy.Lazy

/**
 * Loot table candidates shown in previews. Production always rolls the loaded table instead,
 * so this only has to enumerate the items a table can hand out.
 */
internal object SimulationLootDisplays {
    fun items(
        level: ServerLevel,
        state: BlockState,
    ): List<ItemStack> {
        val table = level.server.reloadableRegistries().getLootTable(state.block.lootTable)
        val context = level.registryAccess().createSerializationContext(JsonOps.INSTANCE)
        val json = encode(table, context) ?: return fallback(state)
        val items = linkedMapOf<Item, ItemStack>()
        collect(json, items)
        return items.values.map(ItemStack::copy).ifEmpty { fallback(state) }
    }

    private fun encode(
        table: LootTable,
        ops: DynamicOps<JsonElement>,
    ): JsonElement? =
        LootTable.DIRECT_CODEC
            .encodeStart(ops, table)
            .resultOrPartial { message -> Lazy.logger.warn("Failed to inspect simulation loot table: {}", message) }
            .orElse(null)

    private fun collect(
        element: JsonElement,
        items: MutableMap<Item, ItemStack>,
    ) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collect(it, items) }
            element.isJsonObject -> {
                val objectValue = element.asJsonObject
                when (objectValue.string("type")) {
                    "minecraft:item" -> objectValue.string("name")?.let { addItem(it, items) }
                    "minecraft:tag" -> objectValue.string("name")?.let { addTag(it, items) }
                }
                objectValue.entrySet().forEach { (_, child) -> collect(child, items) }
            }
        }
    }

    private fun addItem(
        rawId: String,
        items: MutableMap<Item, ItemStack>,
    ) {
        val id = ResourceLocation.tryParse(rawId) ?: return
        BuiltInRegistries.ITEM
            .getOptional(id)
            .orElse(null)
            ?.takeUnless { it === Items.AIR }
            ?.let { items.putIfAbsent(it, ItemStack(it)) }
    }

    private fun addTag(
        rawId: String,
        items: MutableMap<Item, ItemStack>,
    ) {
        val id = ResourceLocation.tryParse(rawId) ?: return
        val tag = TagKey.create(BuiltInRegistries.ITEM.key(), id)
        BuiltInRegistries.ITEM.getTag(tag).orElse(null)?.forEach { holder ->
            holder.value().takeUnless { it === Items.AIR }?.let { items.putIfAbsent(it, ItemStack(it)) }
        }
    }

    private fun fallback(state: BlockState): List<ItemStack> =
        state.block
            .asItem()
            .takeUnless { it === Items.AIR }
            ?.let { listOf(ItemStack(it)) }
            .orEmpty()

    private fun JsonObject.string(name: String): String? = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asString
}

internal fun blockLoot(
    level: Level,
    state: BlockState,
    tool: ItemStack = ItemStack.EMPTY,
) = SimulationBlockLootOutput(
    state,
    (level as? ServerLevel)?.let { SimulationLootDisplays.items(it, state) }.orEmpty(),
    tool,
)
