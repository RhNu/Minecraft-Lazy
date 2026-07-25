package rhx.lazy.curios

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.registry.ModItems
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.SlotResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CuriosIntegrationTest {
    @Test
    fun `teleporter slot predicate accepts only the lazy teleporter`() {
        val predicate =
            requireNotNull(
                CuriosApi
                    .getCurioPredicate(CuriosIntegration.teleporterSlotValidator)
                    .orElse(null),
            )
        val context = SlotContext(CuriosIntegration.TELEPORTER_SLOT, null, 0, false, true)

        assertTrue(predicate.test(SlotResult(context, ItemStack(ModItems.teleporter.get()))))
        assertFalse(predicate.test(SlotResult(context, ItemStack(Items.DIAMOND))))
    }
}
