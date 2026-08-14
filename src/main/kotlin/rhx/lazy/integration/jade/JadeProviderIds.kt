package rhx.lazy.integration.jade

import rhx.lazy.core.lazyId

internal object JadeProviderIds {
    val buffer = lazyId("buffer")
    val energySource = lazyId("energy_source")
    val energySourceStorage = lazyId("energy_source_storage")
    val itemCopier = lazyId("item_copier")
    val repairer = lazyId("repairer")
    val planter = lazyId("planter")
    val simulationChamber = lazyId("simulation_chamber")
    val essenceConverter = lazyId("essence_converter")
}
