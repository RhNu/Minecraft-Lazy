package rhx.lazy.integration.jade

import net.neoforged.fml.ModList
import rhx.lazy.feature.buffer.BufferBlock
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.feature.energy.EnergySourceBlock
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import rhx.lazy.feature.itemcopier.ItemCopierBlock
import rhx.lazy.feature.itemcopier.ItemCopierBlockEntity
import rhx.lazy.feature.repairer.RepairerBlock
import rhx.lazy.feature.repairer.RepairerBlockEntity
import rhx.lazy.integration.jade.botanypots.JadePlanterIntegration
import rhx.lazy.integration.jade.botanypots.client.JadePlanterClientIntegration
import rhx.lazy.integration.jade.client.BufferJadeComponentProvider
import rhx.lazy.integration.jade.client.EnergySourceJadeComponentProvider
import rhx.lazy.integration.jade.client.ItemCopierJadeComponentProvider
import rhx.lazy.integration.jade.client.RepairerJadeComponentProvider
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin

@WailaPlugin
class LazyJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(BufferJadeDataProvider, BufferBlockEntity::class.java)
        registration.registerBlockDataProvider(EnergySourceJadeDataProvider, EnergySourceBlockEntity::class.java)
        registration.registerBlockDataProvider(ItemCopierJadeDataProvider, ItemCopierBlockEntity::class.java)
        registration.registerBlockDataProvider(RepairerJadeDataProvider, RepairerBlockEntity::class.java)
        registration.registerEnergyStorage(EnergySourceStorageHider, EnergySourceBlock::class.java)

        if (ModList.get().isLoaded(BOTANY_POTS_MOD_ID)) {
            JadePlanterIntegration.register(registration)
        }
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(BufferJadeComponentProvider, BufferBlock::class.java)
        registration.registerBlockComponent(EnergySourceJadeComponentProvider, EnergySourceBlock::class.java)
        registration.registerBlockComponent(ItemCopierJadeComponentProvider, ItemCopierBlock::class.java)
        registration.registerBlockComponent(RepairerJadeComponentProvider, RepairerBlock::class.java)

        if (ModList.get().isLoaded(BOTANY_POTS_MOD_ID)) {
            JadePlanterClientIntegration.register(registration)
        }
    }

    private companion object {
        const val BOTANY_POTS_MOD_ID = "botanypots"
    }
}
