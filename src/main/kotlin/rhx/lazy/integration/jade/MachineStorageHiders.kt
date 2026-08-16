package rhx.lazy.integration.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import snownee.jade.api.Accessor
import snownee.jade.api.view.IServerExtensionProvider
import snownee.jade.api.view.ViewGroup

/** Centralized suppression for generic capability rows replaced by a machine's curated Jade view. */
internal object MachineStorageHiders {
    val bufferFluid: IServerExtensionProvider<CompoundTag> =
        EmptyStorageViewProvider(JadeProviderIds.bufferFluidStorage)
    val energySourceEnergy: IServerExtensionProvider<CompoundTag> =
        EmptyStorageViewProvider(JadeProviderIds.energySourceStorage)
    val simulationChamberFluid: IServerExtensionProvider<CompoundTag> =
        EmptyStorageViewProvider(JadeProviderIds.simulationChamberFluidStorage)
    val essenceConverterItems: IServerExtensionProvider<ItemStack> =
        EmptyStorageViewProvider(JadeProviderIds.essenceConverterItemStorage)
}

private class EmptyStorageViewProvider<T>(
    private val uid: ResourceLocation,
) : IServerExtensionProvider<T> {
    override fun getGroups(accessor: Accessor<*>): List<ViewGroup<T>> = emptyList()

    override fun getUid(): ResourceLocation = uid
}
