package rhx.lazy.integration.jade.client

import net.minecraft.resources.ResourceLocation
import rhx.lazy.core.ui.CompactItemCountFormatter
import rhx.lazy.integration.jade.LargeItemStorageProvider
import snownee.jade.api.Accessor
import snownee.jade.api.view.ClientViewGroup
import snownee.jade.api.view.IClientExtensionProvider
import snownee.jade.api.view.ItemView
import snownee.jade.api.view.ViewGroup

/** Restores exact long counts carried by [LargeItemStorageProvider] to Jade's item overlays. */
internal class LargeItemStorageClientProvider(
    private val uid: ResourceLocation,
) : IClientExtensionProvider<net.minecraft.world.item.ItemStack, ItemView> {
    override fun getClientGroups(
        accessor: Accessor<*>,
        groups: List<ViewGroup<net.minecraft.world.item.ItemStack>>,
    ): List<ClientViewGroup<ItemView>> =
        groups.map { group ->
            val amounts = group.extraData.getLongArray(LargeItemStorageProvider.AMOUNTS_TAG)
            val views =
                group.views.mapIndexed { index, stack ->
                    val amount = amounts.getOrElse(index) { stack.count.toLong() }
                    ItemView(stack).apply {
                        if (amount != 1L) amountText(CompactItemCountFormatter.format(amount))
                    }
                }
            ClientViewGroup(views).apply { extraData = group.extraData }
        }

    override fun getUid(): ResourceLocation = uid
}
