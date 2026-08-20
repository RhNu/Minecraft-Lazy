package rhx.lazy.integration

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModList
import rhx.lazy.integration.ae2.Ae2IntegrationModule
import rhx.lazy.integration.appflux.AppliedFluxIntegrationModule
import rhx.lazy.integration.beyonddimensions.BeyondDimensionsIntegrationModule
import rhx.lazy.integration.curios.CuriosIntegrationModule
import rhx.lazy.integration.mekanism.MekanismIntegrationModule
import rhx.lazy.integration.mysticalagriculture.MysticalAgricultureIntegrationModule
import rhx.lazy.integration.silentgear.SilentGearIntegrationModule

internal object LazyIntegrations {
    private val manager =
        IntegrationManager(
            modules =
                listOf(
                    Ae2IntegrationModule,
                    AppliedFluxIntegrationModule,
                    BeyondDimensionsIntegrationModule,
                    SilentGearIntegrationModule,
                    CuriosIntegrationModule,
                    MysticalAgricultureIntegrationModule,
                    MekanismIntegrationModule,
                ),
            isModLoaded = { modId -> ModList.get().isLoaded(modId) },
        )

    fun initialize(modBus: IEventBus) {
        manager.initialize(modBus)
    }

    fun initializeClient(
        modBus: IEventBus,
        gameBus: IEventBus,
    ) {
        manager.initializeClient(modBus, gameBus)
    }
}
