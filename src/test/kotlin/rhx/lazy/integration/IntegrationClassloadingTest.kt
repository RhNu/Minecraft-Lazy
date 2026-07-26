package rhx.lazy.integration

import net.neoforged.bus.api.BusBuilder
import net.neoforged.fml.ModList
import rhx.lazy.core.storage.NetworkStorage
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStorageResult
import rhx.lazy.feature.repairer.ItemRepairHookResult
import rhx.lazy.feature.repairer.ItemRepairHooks
import rhx.lazy.integration.beyonddimensions.BeyondDimensionsIntegrationModule
import rhx.lazy.integration.curios.CuriosIntegrationModule
import rhx.lazy.integration.silentgear.SilentGearIntegrationModule
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.fail

class IntegrationClassloadingTest {
    @Test
    fun `safe bootstraps remain loadable without optional APIs`() {
        OPTIONAL_MODS.forEach { modId -> assertFalse(ModList.get().isLoaded(modId)) }
        OPTIONAL_API_CLASSES.forEach(::assertClassMissing)

        val manager =
            IntegrationManager(
                listOf(
                    BeyondDimensionsIntegrationModule,
                    SilentGearIntegrationModule,
                    CuriosIntegrationModule,
                ),
            ) { false }
        val bus = BusBuilder.builder().build()

        manager.initialize(bus)
        manager.initializeClient(bus, bus)

        assertFalse(NetworkStorage.isAvailable)
        assertSame(
            ItemRepairHookResult.Success,
            ItemRepairHooks.afterRepair(net.minecraft.world.item.ItemStack.EMPTY, null),
        )
        assertSame(
            NetworkStorageResult.Unavailable,
            NetworkStorage.energyAmount(NetworkStorageId(0)),
        )
    }

    private fun assertClassMissing(className: String) {
        try {
            Class.forName(className)
            fail("Expected $className to be absent")
        } catch (_: ClassNotFoundException) {
            // Expected: the standard test runtime deliberately excludes optional integrations.
        }
    }

    private companion object {
        val OPTIONAL_MODS = listOf("beyonddimensions", "silentgear", "curios")
        val OPTIONAL_API_CLASSES =
            listOf(
                "com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet",
                "net.silentchaos512.gear.util.GearData",
                "top.theillusivec4.curios.api.CuriosApi",
            )
    }
}
