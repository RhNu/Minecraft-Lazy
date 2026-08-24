package rhx.lazy.feature.itemcopier

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper

internal object ItemCopierPusher {
    fun pushToHandlers(
        template: ItemStack,
        handlers: Iterable<IItemHandler?>,
    ) {
        if (template.isEmpty) return
        val outputCount = template.maxStackSize.coerceAtLeast(1)
        handlers.forEach { handler ->
            if (handler != null) {
                ItemHandlerHelper.insertItemStacked(
                    handler,
                    template.copyWithCount(outputCount),
                    false,
                )
            }
        }
    }
}
