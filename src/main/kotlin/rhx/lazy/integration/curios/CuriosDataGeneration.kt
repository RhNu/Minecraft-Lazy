package rhx.lazy.integration.curios

import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import top.theillusivec4.curios.api.CuriosDataProvider
import java.util.concurrent.CompletableFuture

internal object CuriosDataGeneration {
    fun gatherData(event: GatherDataEvent) {
        event.generator.addProvider(
            event.includeServer(),
            CuriosData(
                event.generator.packOutput,
                event.existingFileHelper,
                event.lookupProvider,
            ),
        )
    }

    private class CuriosData(
        output: PackOutput,
        helper: ExistingFileHelper,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : CuriosDataProvider(MOD_ID, output, helper, lookup) {
        override fun generate(
            registries: HolderLookup.Provider,
            fileHelper: ExistingFileHelper,
        ) {
            createSlot(CuriosIntegrationModule.TELEPORTER_SLOT)
                .size(1)
                .icon(
                    ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "slot/empty/empty_teleporter_slot",
                    ),
                ).addValidator(CuriosIntegrationModule.teleporterSlotValidator)
            createEntities(CuriosIntegrationModule.TELEPORTER_SLOT)
                .addPlayer()
                .addSlots(CuriosIntegrationModule.TELEPORTER_SLOT)

            createSlot(CuriosIntegrationModule.CONFIGURATION_CARD_SLOT)
                .size(1)
                .icon(
                    ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "slot/empty/empty_configuration_card_slot",
                    ),
                ).addValidator(CuriosIntegrationModule.configurationCardSlotValidator)
            createEntities(CuriosIntegrationModule.CONFIGURATION_CARD_SLOT)
                .addPlayer()
                .addSlots(CuriosIntegrationModule.CONFIGURATION_CARD_SLOT)
        }
    }
}
