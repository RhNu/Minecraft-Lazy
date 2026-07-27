package rhx.lazy.integration.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.Accessor
import snownee.jade.api.view.IServerExtensionProvider
import snownee.jade.api.view.ViewGroup

/**
 * Prevents Jade's generic energy capability view from presenting the energy
 * source's transfer limit as if it were a finite internal buffer.
 */
internal object EnergySourceStorageHider : IServerExtensionProvider<CompoundTag> {
    override fun getGroups(accessor: Accessor<*>): List<ViewGroup<CompoundTag>> = emptyList()

    override fun getUid(): ResourceLocation = JadeProviderIds.energySourceStorage
}
