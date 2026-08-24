package rhx.lazy.integration.curios

import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.MOD_ID
import rhx.lazy.integration.annotation.LazyDataGenContribution
import top.theillusivec4.curios.api.CuriosDataProvider
import java.util.concurrent.CompletableFuture

@LazyDataGenContribution(integrationId = "curios")
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
            createSlot(CuriosDataGenExports.TELEPORTER_SLOT)
                .size(1)
                .icon(
                    ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "slot/empty/empty_teleporter_slot",
                    ),
                ).addValidator(CuriosDataGenExports.teleporterSlotValidator)
            createEntities(CuriosDataGenExports.TELEPORTER_SLOT)
                .addPlayer()
                .addSlots(CuriosDataGenExports.TELEPORTER_SLOT)

            createSlot(CuriosDataGenExports.CONFIGURATION_CARD_SLOT)
                .size(1)
                .icon(
                    ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "slot/empty/empty_configuration_card_slot",
                    ),
                ).addValidator(CuriosDataGenExports.configurationCardSlotValidator)
            createEntities(CuriosDataGenExports.CONFIGURATION_CARD_SLOT)
                .addPlayer()
                .addSlots(CuriosDataGenExports.CONFIGURATION_CARD_SLOT)
        }
    }
}
