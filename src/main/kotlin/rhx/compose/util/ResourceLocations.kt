package rhx.compose.util

import net.minecraft.resources.ResourceLocation
import rhx.compose.MOD_ID

internal fun composeId(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
