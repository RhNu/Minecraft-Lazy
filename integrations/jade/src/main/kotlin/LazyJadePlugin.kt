package rhx.lazy.integration.jade

import rhx.lazy.feature.buffer.BufferBlock
import rhx.lazy.feature.buffer.BufferBlockEntity
import rhx.lazy.feature.energy.EnergySourceBlock
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import rhx.lazy.feature.itemcopier.ItemCopierBlock
import rhx.lazy.feature.itemcopier.ItemCopierBlockEntity
import rhx.lazy.feature.repairer.RepairerBlock
import rhx.lazy.feature.repairer.RepairerBlockEntity
import rhx.lazy.feature.shaping.ShaperBlock
import rhx.lazy.feature.simulation.SimulationChamberBlock
import rhx.lazy.feature.simulation.SimulationChamberBlockEntity
import rhx.lazy.integration.annotation.LazyFrameworkEntrypoint
import rhx.lazy.integration.api.IntegrationModSet
import rhx.lazy.integration.jade.client.BufferJadeComponentProvider
import rhx.lazy.integration.jade.client.EnergySourceJadeComponentProvider
import rhx.lazy.integration.jade.client.ItemCopierJadeComponentProvider
import rhx.lazy.integration.jade.client.LargeItemStorageClientProvider
import rhx.lazy.integration.jade.client.RepairerJadeComponentProvider
import rhx.lazy.integration.jade.client.SimulationChamberJadeComponentProvider
import rhx.lazy.integration.jade.mysticalagriculture.JadeEssenceConverterIntegration
import rhx.lazy.integration.jade.mysticalagriculture.client.JadeEssenceConverterClientIntegration
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin

@WailaPlugin
@LazyFrameworkEntrypoint(key = "lazy")
class LazyJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(BufferJadeDataProvider, BufferBlockEntity::class.java)
        registration.registerBlockDataProvider(EnergySourceJadeDataProvider, EnergySourceBlockEntity::class.java)
        registration.registerBlockDataProvider(ItemCopierJadeDataProvider, ItemCopierBlockEntity::class.java)
        registration.registerBlockDataProvider(RepairerJadeDataProvider, RepairerBlockEntity::class.java)
        registration.registerBlockDataProvider(SimulationChamberJadeDataProvider, SimulationChamberBlockEntity::class.java)
        registration.registerFluidStorage(MachineStorageHiders.bufferFluid, BufferBlock::class.java)
        registration.registerEnergyStorage(MachineStorageHiders.energySourceEnergy, EnergySourceBlock::class.java)
        registration.registerFluidStorage(MachineStorageHiders.simulationChamberFluid, SimulationChamberBlock::class.java)
        registration.registerItemStorage(LargeItemStorageProviders.shaper, ShaperBlock::class.java)
        registration.registerItemStorage(LargeItemStorageProviders.simulationChamber, SimulationChamberBlock::class.java)

        if (IntegrationModSet.isLoaded(MYSTICAL_AGRICULTURE_MOD_ID)) {
            JadeEssenceConverterIntegration.register(registration)
        }
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(BufferJadeComponentProvider, BufferBlock::class.java)
        registration.registerBlockComponent(EnergySourceJadeComponentProvider, EnergySourceBlock::class.java)
        registration.registerBlockComponent(ItemCopierJadeComponentProvider, ItemCopierBlock::class.java)
        registration.registerBlockComponent(RepairerJadeComponentProvider, RepairerBlock::class.java)
        registration.registerBlockComponent(SimulationChamberJadeComponentProvider, SimulationChamberBlock::class.java)
        registration.registerItemStorageClient(LargeItemStorageClientProvider(JadeProviderIds.shaperItemStorage))
        registration.registerItemStorageClient(LargeItemStorageClientProvider(JadeProviderIds.simulationChamberItemStorage))

        if (IntegrationModSet.isLoaded(MYSTICAL_AGRICULTURE_MOD_ID)) {
            JadeEssenceConverterClientIntegration.register(registration)
        }
    }

    private companion object {
        const val MYSTICAL_AGRICULTURE_MOD_ID = "mysticalagriculture"
    }
}
