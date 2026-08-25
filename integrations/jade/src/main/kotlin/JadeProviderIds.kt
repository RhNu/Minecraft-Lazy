package rhx.lazy.integration.jade

import rhx.lazy.core.lazyId

internal object JadeProviderIds {
    val buffer = lazyId("buffer")
    val bufferFluidStorage = lazyId("buffer_fluid_storage")
    val energySource = lazyId("energy_source")
    val energySourceStorage = lazyId("energy_source_storage")
    val replicator = lazyId("replicator")
    val repairer = lazyId("repairer")
    val shaperItemStorage = lazyId("shaper_item_storage")
    val simulationChamber = lazyId("simulation_chamber")
    val simulationChamberItemStorage = lazyId("simulation_chamber_item_storage")
    val simulationChamberFluidStorage = lazyId("simulation_chamber_fluid_storage")
    val essenceConverter = lazyId("essence_converter")
    val essenceConverterItemStorage = lazyId("essence_converter_item_storage")
}
