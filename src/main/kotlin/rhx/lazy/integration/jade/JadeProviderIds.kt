package rhx.lazy.integration.jade

import rhx.lazy.core.lazyId

internal object JadeProviderIds {
    val buffer = lazyId("buffer")
    val bufferFluidStorage = lazyId("buffer_fluid_storage")
    val energySource = lazyId("energy_source")
    val energySourceStorage = lazyId("energy_source_storage")
    val itemCopier = lazyId("item_copier")
    val repairer = lazyId("repairer")
    val simulationChamber = lazyId("simulation_chamber")
    val simulationChamberFluidStorage = lazyId("simulation_chamber_fluid_storage")
    val essenceConverter = lazyId("essence_converter")
    val essenceConverterItemStorage = lazyId("essence_converter_item_storage")
}
