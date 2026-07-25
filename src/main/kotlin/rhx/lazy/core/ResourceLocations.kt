package rhx.lazy.core

import net.minecraft.resources.ResourceLocation
import rhx.lazy.MOD_ID

internal fun lazyId(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
