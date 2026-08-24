package rhx.lazy.core.process

import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ResourceStore
import rhx.lazy.core.resource.fluidAmount
import rhx.lazy.core.resource.itemAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkControllerTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `prepared result survives persistence and resumes without rerolling`() {
        val items = ResourceStore(ItemResourceKind, 1, 10)
        val fluids = ResourceStore(FluidResourceKind, 1, 10)
        items.insert(requireNotNull(itemAmount(ItemStack(Items.STONE), 10)))
        val original =
            PreparedCommit(
                listOf(requireNotNull(itemAmount(ItemStack(Items.DIAMOND), 5))),
                listOf(requireNotNull(fluidAmount(FluidStack(Fluids.WATER, 1), 7))),
                3,
            )

        assertFalse(original.drainInto(items, fluids))
        val restored = assertNotNull(PreparedCommit.parse(registries, original.save(registries)))
        assertEquals(3, restored.workUnits)

        items.clear()
        assertTrue(restored.drainInto(items, fluids))
        assertEquals(5L, items.amount(0))
        assertEquals(7L, fluids.amount(0))
    }

    @Test
    fun `blocked controller never asks provider for another result`() {
        val items = ResourceStore(ItemResourceKind, 1, 1)
        val fluids = ResourceStore(FluidResourceKind, 1, 1)
        items.insert(requireNotNull(itemAmount(ItemStack(Items.STONE), 1)))
        var generated = 0
        val provider =
            object : WorkProvider {
                override fun step(workBudget: Int): WorkStep {
                    generated++
                    return WorkStep.Produced(
                        PreparedCommit(listOf(requireNotNull(itemAmount(ItemStack(Items.DIAMOND), 1))), emptyList(), 1),
                    )
                }

                override fun committed(workUnits: Int) = Unit
            }
        val controller = WorkController(provider) { it.drainInto(items, fluids) }

        controller.tick(1)
        controller.tick(1)

        assertEquals(1, generated)
        assertEquals(WorkStatus.BLOCKED, controller.status)
    }
}
