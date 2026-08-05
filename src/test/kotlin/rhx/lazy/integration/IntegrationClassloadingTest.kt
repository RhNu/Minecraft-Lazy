package rhx.lazy.integration

import net.neoforged.bus.api.BusBuilder
import net.neoforged.fml.ModList
import rhx.lazy.feature.repairer.ItemRepairHookResult
import rhx.lazy.feature.repairer.ItemRepairHooks
import rhx.lazy.integration.ae2.Ae2IntegrationModule
import rhx.lazy.integration.appflux.AppliedFluxIntegrationModule
import rhx.lazy.integration.beyonddimensions.BeyondDimensionsIntegrationModule
import rhx.lazy.integration.botanypots.BotanyPotsIntegrationModule
import rhx.lazy.integration.curios.CuriosIntegrationModule
import rhx.lazy.integration.mysticalagriculture.MysticalAgricultureIntegrationModule
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
                    Ae2IntegrationModule,
                    AppliedFluxIntegrationModule,
                    BeyondDimensionsIntegrationModule,
                    BotanyPotsIntegrationModule,
                    SilentGearIntegrationModule,
                    CuriosIntegrationModule,
                    MysticalAgricultureIntegrationModule,
                ),
            ) { false }
        val bus = BusBuilder.builder().build()

        manager.initialize(bus)
        manager.initializeClient(bus, bus)

        assertSame(
            ItemRepairHookResult.Success,
            ItemRepairHooks.afterRepair(net.minecraft.world.item.ItemStack.EMPTY, null),
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
        val OPTIONAL_MODS =
            listOf(
                "beyonddimensions",
                "botanypots",
                "silentgear",
                "curios",
                "jade",
                "mysticalagriculture",
                "mysticalagradditions",
                "ae2",
                "appflux",
                "guideme",
                "glodium",
            )
        val OPTIONAL_API_CLASSES =
            listOf(
                "com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet",
                "net.darkhax.botanypots.common.api.context.BotanyPotContext",
                "net.silentchaos512.gear.util.GearData",
                "top.theillusivec4.curios.api.CuriosApi",
                "snownee.jade.api.IWailaPlugin",
                "com.blakebr0.mysticalagriculture.MysticalAgriculture",
                "com.blakebr0.mysticalagradditions.MysticalAgradditions",
                "appeng.api.features.GridLinkables",
                "com.glodblock.github.appflux.common.me.key.FluxKey",
                "guideme.Guide",
                "com.glodblock.github.glodium.Glodium",
            )
    }
}
