package rhx.lazy.feature.repairer

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepairerBlockEntityTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `slot only accepts one damaged durability item`() {
        val repairer = newRepairer()
        val damagedPickaxes = damagedStack(Items.DIAMOND_PICKAXE, damage = 100, count = 3)

        val remainder = repairer.itemHandler.insertItem(0, damagedPickaxes, false)

        assertEquals(1, repairer.itemHandler.getStackInSlot(0).count)
        assertEquals(100, repairer.itemHandler.getStackInSlot(0).damageValue)
        assertEquals(2, remainder.count)

        val secondDamagedItem = damagedStack(Items.IRON_SHOVEL, damage = 10)
        assertEquals(secondDamagedItem, repairer.itemHandler.insertItem(0, secondDamagedItem, false))
    }

    @Test
    fun `slot rejects empty full durability and non durability items`() {
        val repairer = newRepairer()
        val fullPickaxe = ItemStack(Items.DIAMOND_PICKAXE)
        val stone = ItemStack(Items.STONE)

        assertFalse(repairer.itemHandler.isItemValid(0, ItemStack.EMPTY))
        assertFalse(repairer.itemHandler.isItemValid(0, fullPickaxe))
        assertFalse(repairer.itemHandler.isItemValid(0, stone))
        assertEquals(fullPickaxe, repairer.itemHandler.insertItem(0, fullPickaxe, false))
        assertEquals(stone, repairer.itemHandler.insertItem(0, stone, false))
        assertTrue(repairer.itemHandler.getStackInSlot(0).isEmpty)
    }

    @Test
    fun `simulated item transfers never change either side`() {
        val repairer = newRepairer()
        val pickaxe = damagedStack(Items.IRON_PICKAXE, damage = 80)

        assertTrue(repairer.itemHandler.insertItem(0, pickaxe, true).isEmpty)
        assertTrue(repairer.itemHandler.getStackInSlot(0).isEmpty)
        assertEquals(1, pickaxe.count)

        repairer.itemHandler.insertItem(0, pickaxe, false)
        val simulatedExtraction = repairer.itemHandler.extractItem(0, 1, true)

        assertEquals(80, simulatedExtraction.damageValue)
        assertEquals(80, repairer.itemHandler.getStackInSlot(0).damageValue)
    }

    @Test
    fun `managed data preserves item components and damage`() {
        val source = newRepairer()
        val pickaxe = damagedStack(Items.DIAMOND_PICKAXE, damage = 321)
        pickaxe.set(DataComponents.CUSTOM_NAME, Component.literal("Patient pickaxe"))
        source.itemHandler.insertItem(0, pickaxe, false)

        val restored = newRepairer()
        restored.loadWithComponents(source.saveWithFullMetadata(registries), registries)

        val stored = restored.itemHandler.getStackInSlot(0)
        assertEquals(1, stored.count)
        assertEquals(321, stored.damageValue)
        assertEquals("Patient pickaxe", stored.hoverName.string)
    }

    @Test
    fun `repair amount rounds upward including tools below one hundred durability`() {
        assertEquals(1, RepairerBlockEntity.repairAmount(1, 1))
        assertEquals(1, RepairerBlockEntity.repairAmount(20, 5))
        assertEquals(3, RepairerBlockEntity.repairAmount(20, 15))
        assertEquals(5, RepairerBlockEntity.repairAmount(99, 5))
        assertEquals(15, RepairerBlockEntity.repairAmount(99, 15))
        assertEquals(16, RepairerBlockEntity.repairAmount(1_561, 1))
        assertEquals(1_561, RepairerBlockEntity.repairAmount(1_561, 100))
        assertEquals(16, RepairerBlockEntity.repairAmount(1_561, 0))
        assertEquals(1_561, RepairerBlockEntity.repairAmount(1_561, 101))
    }

    @Test
    fun `repair range accepts reversed config values and clamps invalid values`() {
        assertEquals(5..15, RepairerBlockEntity.normalizedRepairPercentRange(15, 5))
        assertEquals(1..100, RepairerBlockEntity.normalizedRepairPercentRange(-10, 150))
        assertEquals(100..100, RepairerBlockEntity.normalizedRepairPercentRange(120, 110))
    }

    @Test
    fun `each press repairs once using the configured inclusive range`() {
        val repairer = newRepairer()
        repairer.itemHandler.insertItem(0, damagedStack(Items.IRON_PICKAXE, damage = 100), false)
        val random = RandomSource.create(42)

        assertTrue(repairer.repairOnce(5, 5, random))
        assertEquals(87, repairer.itemHandler.getStackInSlot(0).damageValue)

        assertTrue(repairer.repairOnce(5, 5, random))
        assertEquals(74, repairer.itemHandler.getStackInSlot(0).damageValue)
    }

    @Test
    fun `a low durability tool still repairs by at least one point`() {
        val repairer = newRepairer()
        repairer.itemHandler.insertItem(0, damagedStack(Items.FISHING_ROD, damage = 10), false)

        assertTrue(repairer.repairOnce(5, 5, RandomSource.create(11)))

        assertEquals(6, repairer.itemHandler.getStackInSlot(0).damageValue)
    }

    @Test
    fun `repair clamps at full durability and the result remains extractable`() {
        val repairer = newRepairer()
        repairer.itemHandler.insertItem(0, damagedStack(Items.IRON_PICKAXE, damage = 2), false)

        assertTrue(repairer.repairOnce(15, 15, RandomSource.create(7)))
        assertFalse(repairer.hasRepairableItem())

        val extracted = repairer.itemHandler.extractItem(0, 1, false)
        assertEquals(0, extracted.damageValue)
        assertTrue(repairer.itemHandler.getStackInSlot(0).isEmpty)
    }

    @Test
    fun `menu replacement can preserve a fully repaired item without duplicating it`() {
        val repairer = newRepairer()
        val repairedPickaxe = ItemStack(Items.IRON_PICKAXE)

        repairer.itemHandler.setStackInSlot(0, repairedPickaxe)

        val stored = repairer.itemHandler.getStackInSlot(0)
        assertTrue(ItemStack.isSameItemSameComponents(repairedPickaxe, stored))
        assertEquals(1, stored.count)

        val extracted = repairer.itemHandler.extractItem(0, 1, false)
        assertTrue(ItemStack.isSameItemSameComponents(repairedPickaxe, extracted))
        assertEquals(1, extracted.count)
        assertTrue(repairer.itemHandler.getStackInSlot(0).isEmpty)
    }

    @Test
    fun `taking item for block removal clears stored contents`() {
        val repairer = newRepairer()
        repairer.itemHandler.insertItem(0, damagedStack(Items.NETHERITE_HOE, damage = 42), false)

        val dropped = repairer.takeStoredItemForDrop()

        assertEquals(Items.NETHERITE_HOE, dropped.item)
        assertEquals(42, dropped.damageValue)
        assertTrue(repairer.itemHandler.getStackInSlot(0).isEmpty)
        assertTrue(repairer.takeStoredItemForDrop().isEmpty)
    }

    private fun newRepairer(): RepairerBlockEntity =
        RepairerBlockEntity(
            BlockPos.ZERO,
            RepairerRegistries.block.get().defaultBlockState(),
        )

    private fun damagedStack(
        item: net.minecraft.world.item.Item,
        damage: Int,
        count: Int = 1,
    ): ItemStack =
        ItemStack(item, count).apply {
            damageValue = damage
        }
}
