package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationDisplayIconsTest {
    @Test
    fun `an item target is its own icon`() {
        val icon = SimulationDisplayIcons.iconFor(ItemStack(Items.WHEAT_SEEDS, 3))

        assertEquals(Items.WHEAT_SEEDS, icon.item)
        assertEquals(1, icon.count)
    }

    @Test
    fun `an empty target shows nothing`() {
        assertTrue(SimulationDisplayIcons.iconFor(ItemStack.EMPTY).isEmpty)
    }

    @Test
    fun `a bound data model shows the spawn egg of its entity`() {
        val icon = SimulationDisplayIcons.iconFor(dataModel(EntityType.COW))

        assertEquals(Items.COW_SPAWN_EGG, icon.item)
    }

    @Test
    fun `a spawn egg shows the same entity icon as a bound data model`() {
        val eggIcon = SimulationDisplayIcons.iconFor(ItemStack(Items.COW_SPAWN_EGG, 3))
        val modelIcon = SimulationDisplayIcons.iconFor(dataModel(EntityType.COW))

        assertTrue(ItemStack.isSameItemSameComponents(modelIcon, eggIcon))
        assertEquals(1, eggIcon.count)
    }

    @Test
    fun `a blank data model stays the card`() {
        val icon = SimulationDisplayIcons.iconFor(ItemStack(SimulationRegistries.dataModelItem.get()))

        assertEquals(SimulationRegistries.dataModelItem.get(), icon.item)
    }

    @Test
    fun `an entity without a spawn egg falls back to the card`() {
        val icon = SimulationDisplayIcons.iconFor(dataModel(EntityType.ILLUSIONER))

        assertEquals(SimulationRegistries.dataModelItem.get(), icon.item)
    }

    @Test
    fun `a binding left behind by a removed mod falls back to the card`() {
        val model =
            ItemStack(SimulationRegistries.dataModelItem.get()).apply {
                set(SimulationRegistries.entityTypeComponent.get(), ResourceLocation.fromNamespaceAndPath("lazy", "absent_mob"))
            }

        assertEquals(SimulationRegistries.dataModelItem.get(), SimulationDisplayIcons.iconFor(model).item)
    }

    private fun dataModel(entity: EntityType<*>): ItemStack =
        ItemStack(SimulationRegistries.dataModelItem.get()).apply {
            set(SimulationRegistries.entityTypeComponent.get(), BuiltInRegistries.ENTITY_TYPE.getKey(entity))
        }
}
