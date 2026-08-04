package rhx.lazy.integration.mysticalagriculture

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

internal enum class EssenceTier(
    val serializedName: String,
    val itemId: ResourceLocation,
    val inferiumValue: Int,
) {
    INFERIUM(
        "inferium",
        ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "inferium_essence"),
        1,
    ),
    PRUDENTIUM(
        "prudentium",
        ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "prudentium_essence"),
        4,
    ),
    TERTIUM(
        "tertium",
        ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "tertium_essence"),
        16,
    ),
    IMPERIUM(
        "imperium",
        ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "imperium_essence"),
        64,
    ),
    SUPREMIUM(
        "supremium",
        ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "supremium_essence"),
        256,
    ),
    INSANIUM(
        "insanium",
        ResourceLocation.fromNamespaceAndPath("mysticalagradditions", "insanium_essence"),
        1_024,
    ),
    ;

    fun itemOrNull(): Item? = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null)

    fun isAvailable(): Boolean = itemOrNull() != null

    fun createStack(count: Int = 1): ItemStack = itemOrNull()?.let { ItemStack(it, count) } ?: ItemStack.EMPTY

    companion object {
        fun fromSerializedName(name: String): EssenceTier? = entries.firstOrNull { it.serializedName == name }

        fun fromStack(stack: ItemStack): EssenceTier? {
            if (stack.isEmpty || !stack.componentsPatch.isEmpty) return null
            val id = BuiltInRegistries.ITEM.getKey(stack.item)
            return entries.firstOrNull { it.itemId == id && it.isAvailable() }
        }
    }
}
