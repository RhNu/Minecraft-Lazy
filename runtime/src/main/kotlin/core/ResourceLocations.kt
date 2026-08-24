package rhx.lazy.core

import net.minecraft.resources.ResourceLocation
import rhx.lazy.MOD_ID
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public fun lazyId(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
