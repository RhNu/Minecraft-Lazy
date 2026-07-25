package rhx.lazy.integration.repair

import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepairCompatibilitiesTest {
    @Test
    fun `compatibility remains loadable when silent gear is absent`() {
        assertFalse(ModList.get().isLoaded(SILENT_GEAR_MOD_ID))
        var apiClassIsMissing = false
        try {
            Class.forName(SILENT_GEAR_API_CLASS)
        } catch (_: ClassNotFoundException) {
            apiClassIsMissing = true
        }
        assertTrue(apiClassIsMissing)

        RepairCompatibilities.init()
        RepairCompatibilities.afterRepair(ItemStack.EMPTY, null)
    }

    private companion object {
        const val SILENT_GEAR_MOD_ID = "silentgear"
        const val SILENT_GEAR_API_CLASS = "net.silentchaos512.gear.util.GearData"
    }
}
