package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntitySimulationTargetsTest {
    @Test
    fun `spawn egg and bound data model resolve to the same entity target`() {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW)

        val eggResolution = EntitySimulationTargets.resolve(ItemStack(Items.COW_SPAWN_EGG))
        val modelResolution = EntitySimulationTargets.resolve(DataModelItem.boundTo(entityId))

        assertTrue(eggResolution is EntitySimulationTargetResolution.Resolved)
        assertTrue(modelResolution is EntitySimulationTargetResolution.Resolved)
        val eggTarget = eggResolution.target
        val modelTarget = modelResolution.target

        assertEquals(entityId, eggTarget.id)
        assertEquals(eggTarget, modelTarget)
    }

    @Test
    fun `dragon egg resolves to the ender dragon target`() {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON)

        val resolution = EntitySimulationTargets.resolve(ItemStack(Items.DRAGON_EGG))

        assertTrue(resolution is EntitySimulationTargetResolution.Resolved)
        assertEquals(entityId, resolution.target.id)
        assertEquals(EntityType.ENDER_DRAGON, resolution.target.type)
    }

    @Test
    fun `removed entity binding stays an invalid entity target`() {
        val model =
            DataModelItem.boundTo(
                ResourceLocation.fromNamespaceAndPath("lazy_test", "removed_entity"),
            )

        assertEquals(
            EntitySimulationTargetResolution.InvalidEntityTarget,
            EntitySimulationTargets.resolve(model),
        )
    }

    @Test
    fun `equivalent inputs include spawn egg and bound data model`() {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW)

        val inputs = EntitySimulationTargets.equivalentInputs(entityId)

        assertEquals(2, inputs.size)
        assertEquals(Items.COW_SPAWN_EGG, inputs.first().item)
        assertEquals(entityId, DataModelItem.entityTypeId(inputs.last()))
    }

    @Test
    fun `entity without a spawn egg still has its bound model input`() {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ILLUSIONER)

        val inputs = EntitySimulationTargets.equivalentInputs(entityId)

        assertEquals(1, inputs.size)
        assertTrue(inputs.single().`is`(SimulationRegistries.dataModelItem.get()))
        assertEquals(entityId, DataModelItem.entityTypeId(inputs.single()))
    }

    @Test
    fun `ender dragon inputs include dragon egg and bound data model`() {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON)

        val inputs = EntitySimulationTargets.equivalentInputs(entityId)

        assertTrue(inputs.any { it.`is`(Items.DRAGON_EGG) })
        assertEquals(entityId, DataModelItem.entityTypeId(inputs.last()))
    }
}
