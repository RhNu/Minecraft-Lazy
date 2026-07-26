package rhx.lazy.integration.botanypots

import net.darkhax.bookshelf.common.api.function.ReloadableCache
import net.darkhax.botanypots.common.api.data.components.CropOverride
import net.darkhax.botanypots.common.api.data.components.SoilOverride
import net.darkhax.botanypots.common.api.data.recipes.BotanyPotRecipe
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil
import net.darkhax.botanypots.common.impl.BotanyPotsMod
import net.darkhax.botanypots.common.impl.data.recipe.crop.BasicCrop
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.level.Level

internal class PlanterRecipeResolver(
    private val context: PlanterBotanyContext,
) {
    private val soilCache =
        ReloadableCache.of<RecipeHolder<Soil>> { level ->
            SoilOverride
                .get(context.soilItem)
                .map { override -> RecipeHolder(BotanyPotsMod.BUILTIN_COMPONENT_ID, override.soil()) }
                .orElseGet {
                    Soil.CACHE.apply(level)?.lookup(context.soilItem, context, level)
                }
        }

    private val cropCache =
        ReloadableCache.of<RecipeHolder<Crop>> { level ->
            CropOverride
                .get(context.seedItem)
                .map { override -> RecipeHolder(BotanyPotsMod.BUILTIN_COMPONENT_ID, override.crop()) }
                .orElseGet {
                    Crop.CACHE.apply(level)?.lookup(context.seedItem, context, level)
                }
        }

    var activeCrop: Crop? = null
        private set

    var activeSoil: Soil? = null
        private set

    fun resolveSoil(level: Level): Soil? = resolve(context.soilItem, soilCache, level).also { activeSoil = it }

    fun resolveCrop(level: Level): Crop? {
        val crop = resolve(context.seedItem, cropCache, level)
        val resolved =
            crop?.takeIf {
                if (matchesInsertedPotPredicate(it)) {
                    true
                } else {
                    cropCache.invalidate()
                    false
                }
            }
        activeCrop = resolved
        return resolved
    }

    fun isValidSoil(
        level: Level,
        stack: ItemStack,
    ): Boolean =
        SoilOverride.get(stack).isPresent ||
            Soil.CACHE.apply(level)?.lookup(stack, context, level) != null

    fun isValidCrop(
        level: Level,
        stack: ItemStack,
    ): Boolean =
        CropOverride.get(stack).isPresent ||
            Crop.CACHE.apply(level)?.lookup(stack, context, level) != null

    fun invalidate() {
        soilCache.invalidate()
        cropCache.invalidate()
        clearActive()
    }

    fun clearActive() {
        activeCrop = null
        activeSoil = null
    }

    private fun <T : BotanyPotRecipe> resolve(
        stack: ItemStack,
        cache: ReloadableCache<RecipeHolder<T>>,
        level: Level,
    ): T? {
        if (stack.isEmpty) {
            cache.invalidate()
            return null
        }
        val recipe = cache.apply(level)?.value() ?: return null
        if (!recipe.matches(context, level)) {
            cache.invalidate()
            return null
        }
        return recipe
    }

    private fun matchesInsertedPotPredicate(crop: Crop): Boolean {
        val predicate = (crop as? BasicCrop)?.basicProperties?.potPredicate()?.orElse(null) ?: return true
        if (predicate.requiresNbt()) return false
        val state = context.insertedPotBlockState() ?: return false
        return predicate.matches(context.insertedPotInWorld(state))
    }
}
