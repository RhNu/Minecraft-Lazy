package rhx.lazy.integration.mysticalagriculture

import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.level.block.Block
import rhx.lazy.integration.api.LazyInternalApi

/** DataGen-safe view of the optional Essence Converter implementation. */
@LazyInternalApi
public object MysticalAgricultureDataGenExports {
    public const val MOD_ID: String = "mysticalagriculture"

    public fun essenceConverterBlock(): Block = EssenceConverterRegistries.block.get()

    public fun essenceConverterItem(): Item = EssenceConverterRegistries.item.get()

    public fun consumingRecipe(recipe: ShapedRecipe): Recipe<*> = ConsumingShapedRecipe.fromShaped(recipe)
}
