package rhx.lazy.integration.jade

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.items.IItemHandler
import rhx.lazy.core.resource.ResourceItemHandler
import rhx.lazy.feature.shaping.ShaperBlockEntity
import rhx.lazy.feature.simulation.SimulationChamberBlockEntity
import snownee.jade.api.Accessor
import snownee.jade.api.view.IServerExtensionProvider
import snownee.jade.api.view.ViewGroup

/**
 * Jade item-storage view that keeps the wire [ItemStack] legal and carries its exact [Long] amount
 * beside it. The matching client extension replaces Jade's count overlay with this value.
 */
internal class LargeItemStorageProvider<E : BlockEntity>(
    private val entityClass: Class<E>,
    private val uid: ResourceLocation,
    private val handlers: (E) -> List<IItemHandler>,
) : IServerExtensionProvider<ItemStack> {
    override fun getGroups(accessor: Accessor<*>): List<ViewGroup<ItemStack>> {
        val target = accessor.target ?: return emptyList()
        if (!entityClass.isInstance(target)) return emptyList()
        return handlers(entityClass.cast(target)).mapNotNull(::group)
    }

    override fun getUid(): ResourceLocation = uid

    private fun group(handler: IItemHandler): ViewGroup<ItemStack>? {
        val items = mutableListOf<ItemStack>()
        val amounts = mutableListOf<Long>()
        for (slot in 0 until handler.slots) {
            val stack = handler.getStackInSlot(slot)
            if (stack.isEmpty) continue
            val amount =
                if (handler is ResourceItemHandler) {
                    handler.getAmountInSlot(slot)
                } else {
                    stack.count.toLong()
                }
            if (amount <= 0L) continue
            items += stack.copyWithCount(1)
            amounts += amount
        }
        if (items.isEmpty()) return null
        return ViewGroup(items).apply {
            extraData.putLongArray(AMOUNTS_TAG, amounts)
        }
    }

    internal companion object {
        const val AMOUNTS_TAG = "LazyAmounts"
    }
}

internal object LargeItemStorageProviders {
    val shaper =
        LargeItemStorageProvider(
            ShaperBlockEntity::class.java,
            JadeProviderIds.shaperItemStorage,
        ) { entity -> listOf(entity.inputHandler, entity.outputHandler) }

    val simulationChamber =
        LargeItemStorageProvider(
            SimulationChamberBlockEntity::class.java,
            JadeProviderIds.simulationChamberItemStorage,
        ) { entity -> listOf(entity.inputItemHandler, entity.outputItemHandler) }
}
