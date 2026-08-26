package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import rhx.lazy.core.lazyId
import rhx.lazy.integration.api.LazyInternalApi

/** Stable grouping key shared by every source in the unified simulation variant registry. */
@LazyInternalApi
public object SimulationRecipeGroups {
    public val ITEM: ResourceLocation = lazyId("item")
    public val AUTOMATIC: ResourceLocation = lazyId("automatic")
    public val ENTITY: ResourceLocation = lazyId("entity")
    public val INJECTION: ResourceLocation = lazyId("injection")
}
