package rhx.lazy.integration.mysticalagriculture

import com.mojang.serialization.MapCodec
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapedRecipePattern
import java.util.stream.Stream

internal class ConsumingShapedRecipe(
    group: String,
    category: CraftingBookCategory,
    pattern: ShapedRecipePattern,
    result: ItemStack,
    showNotification: Boolean,
) : ShapedRecipe(group, category, pattern, result, showNotification) {
    override fun getSerializer(): RecipeSerializer<*> = EssenceConverterRegistries.consumingShapedRecipe.get()

    override fun getRemainingItems(input: CraftingInput): NonNullList<ItemStack> {
        val remaining = super.getRemainingItems(input)
        for (slot in 0 until input.size()) {
            val stack = input.getItem(slot)
            if (!stack.isEmpty && BuiltInRegistries.ITEM.getKey(stack.item) == MASTER_INFUSION_CRYSTAL_ID) {
                remaining[slot] = ItemStack.EMPTY
            }
        }
        return remaining
    }

    internal class Serializer : RecipeSerializer<ConsumingShapedRecipe> {
        override fun codec(): MapCodec<ConsumingShapedRecipe> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ConsumingShapedRecipe> = STREAM_CODEC
    }

    companion object {
        private val EMPTY_LOOKUP = HolderLookup.Provider.create(Stream.empty())

        val CODEC: MapCodec<ConsumingShapedRecipe> =
            ShapedRecipe.Serializer.CODEC.xmap(::fromShaped) { recipe -> recipe }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ConsumingShapedRecipe> =
            StreamCodec.of(
                { buffer, recipe -> ShapedRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe) },
                { buffer -> fromShaped(ShapedRecipe.Serializer.STREAM_CODEC.decode(buffer)) },
            )

        private val MASTER_INFUSION_CRYSTAL_ID =
            ResourceLocation.fromNamespaceAndPath("mysticalagriculture", "master_infusion_crystal")

        fun fromShaped(recipe: ShapedRecipe): ConsumingShapedRecipe =
            ConsumingShapedRecipe(
                recipe.group,
                recipe.category(),
                recipe.pattern,
                recipe.getResultItem(EMPTY_LOOKUP).copy(),
                recipe.showNotification(),
            )
    }
}
