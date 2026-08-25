package rhx.lazy.feature.replicator

import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.ManagedBlockEntity
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceContainerExtractors
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceKinds
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.core.testing.FakeNetworkOutputProvider
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplicatorBlockEntityTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `item ghost marks identity while amount is edited independently`() {
        val replicator = newReplicator()
        val source = ItemStack(Items.DIAMOND, 37)
        source.set(DataComponents.CUSTOM_NAME, Component.literal("Original diamond"))

        replicator.setItemTemplate(source)

        assertEquals(37, source.count)
        assertEquals(1, replicator.getResource()?.amount)
        assertEquals(1, replicator.getItemTemplate().count)
        assertEquals("Original diamond", replicator.getItemTemplate().hoverName.string)
        assertTrue(replicator.hasResource())

        replicator.setAmount(500L)
        replicator.setItemTemplate(source.copyWithCount(2))

        assertEquals(500L, replicator.getResource()?.amount)

        replicator.clearResource()

        assertNull(replicator.getResource())
        assertFalse(replicator.hasResource())
    }

    @Test
    fun `fluid ghost selection replaces an item selection through the abstract resource API`() {
        val replicator = newReplicator()
        replicator.setItemTemplate(ItemStack(Items.DIAMOND, 4))
        val water = FluidStack(Fluids.WATER, 8_000)
        water.set(DataComponents.CUSTOM_NAME, Component.literal("Copied water"))

        replicator.setFluidTemplate(water)

        assertTrue(replicator.getItemTemplate().isEmpty)
        assertEquals(1_000L, replicator.getResource()?.amount)
        assertEquals(1_000, replicator.getFluidTemplate().amount)
        assertEquals("Copied water", replicator.getFluidTemplate().get(DataComponents.CUSTOM_NAME)?.string)
        assertTrue(replicator.getResource()?.variant is FluidVariant)
    }

    @Test
    fun `container extraction marks a fluid bucket without changing the carried item`() {
        val bucket = ItemStack(Items.WATER_BUCKET)

        val selected = assertNotNull(ResourceContainerExtractors.extractFirst(bucket))

        assertEquals(ResourceKinds.FLUID, selected.kind)
        assertEquals(ResourceKinds.FLUID.defaultAmount, selected.amount)
        assertEquals(Fluids.WATER, (selected.variant as FluidVariant).template.fluid)
        assertEquals(Items.WATER_BUCKET, bucket.item)
        assertEquals(1, bucket.count)
    }

    @Test
    fun `amount controls use each resource kind default unit and clamp safely`() {
        val replicator = newReplicator()
        replicator.setItemTemplate(ItemStack(Items.DIAMOND, 64))

        assertEquals(1L, replicator.amountStep())
        replicator.adjustAmount(-1L)
        assertEquals(1L, replicator.getResource()?.amount)
        replicator.adjustAmount(36L)
        assertEquals(37L, replicator.getResource()?.amount)

        replicator.setFluidTemplate(FluidStack(Fluids.WATER, 250))
        assertEquals(1_000L, replicator.amountStep())
        assertEquals(1_000L, replicator.getResource()?.amount)
        replicator.setAmount(Long.MAX_VALUE)
        replicator.adjustAmount(1_000L)
        assertEquals(Long.MAX_VALUE, replicator.getResource()?.amount)

        replicator.setFluidTemplate(FluidStack(Fluids.WATER, 1))
        assertEquals(Long.MAX_VALUE, replicator.getResource()?.amount)
        replicator.setFluidTemplate(FluidStack(Fluids.LAVA, 1))
        assertEquals(1_000L, replicator.getResource()?.amount)
    }

    @Test
    fun `public resource boundary copies variants defensively`() {
        val replicator = newReplicator()
        val source = ItemStack(Items.EMERALD, 12)
        val variant = requireNotNull(ItemVariant.of(source))

        replicator.setResource(ResourceAmount(ResourceKinds.ITEM, variant, 500L))
        source.count = 1

        val selected = requireNotNull(replicator.getResource())
        assertEquals(500L, selected.amount)
        assertTrue(selected.variant is ItemVariant)
        assertEquals(Items.EMERALD, (selected.variant as ItemVariant).template.item)
    }

    @Test
    fun `replicator accepts an integration defined resource kind`() {
        val replicator = newReplicator()
        val variant = TestVariant("chemical-like")

        replicator.markResource(TestResourceKind, variant)

        assertEquals(TestResourceKind.defaultAmount, replicator.getResource()?.amount)
        assertEquals(setOf(TestResourceKind), replicator.ioController.capabilities)
        assertTrue(replicator.getResource()?.matches(ResourceAmount(TestResourceKind, variant, 1L)) == true)
    }

    @Test
    fun `selected kind drives capabilities and fluid network output`() {
        val replicator = newReplicator()
        val storage = FakeNetworkStorage()
        val provider = FakeNetworkOutputProvider(storage)

        assertEquals(emptySet(), replicator.ioController.capabilities)
        replicator.setFluidTemplate(FluidStack(Fluids.WATER, 8_000))
        replicator.setAmount(8_000L)

        assertEquals(setOf(ResourceKinds.FLUID), replicator.ioController.capabilities)
        assertTrue(replicator.ioController.setNetworkTarget(provider.target))
        replicator.onServerTick()

        assertEquals(8_000L, storage.storedFluidAmount)
        assertEquals(Fluids.WATER, storage.storedFluid.fluid)
    }

    @Test
    fun `gear cycles and schedule resets immediately`() {
        val replicator = newReplicator()

        assertEquals(20, replicator.getGear().intervalTicks)
        assertTrue(replicator.advanceSchedule())
        repeat(ReplicatorGear.DEFAULT.intervalTicks - 1) {
            assertFalse(replicator.advanceSchedule())
        }
        assertTrue(replicator.advanceSchedule())

        replicator.setGear(ReplicatorGear.FAST)
        assertTrue(replicator.advanceSchedule())
        assertEquals(10, replicator.getGear().intervalTicks)
    }

    @Test
    fun `block item round trip preserves a fluid resource and gear`() {
        val source = newReplicator()
        val template = FluidStack(Fluids.LAVA, 4_000)
        template.set(DataComponents.CUSTOM_NAME, Component.literal("Copied lava"))
        source.setFluidTemplate(template)
        source.setAmount(4_000L)
        source.setGear(ReplicatorGear.VERY_SLOW)

        val dropped = ItemStack(ReplicatorRegistries.item.get())
        source.saveContentsToItem(dropped, registries)
        val blockEntityData = requireNotNull(dropped.get(DataComponents.BLOCK_ENTITY_DATA))
        val serialized =
            requireNotNull(
                ResourceAmount.parse(
                    registries,
                    blockEntityData.copyTag().getCompound(ReplicatorBlockEntity.RESOURCE_TAG),
                ),
            )
        val managed = blockEntityData.copyTag().getCompound(ManagedBlockEntity.MANAGED_DATA_KEY)

        val restored = newReplicator()
        restored.loadWithComponents(blockEntityData.copyTag(), registries)

        assertEquals(4_000L, serialized.amount)
        assertTrue(serialized.variant is FluidVariant)
        assertEquals(ReplicatorGear.VERY_SLOW.intervalTicks, managed.getInt(ReplicatorBlockEntity.PUSH_INTERVAL_FIELD))
        assertEquals("Copied lava", restored.getFluidTemplate().get(DataComponents.CUSTOM_NAME)?.string)
        assertEquals(1_000, restored.getFluidTemplate().amount)
        assertEquals(4_000L, restored.getResource()?.amount)
        assertEquals(ReplicatorGear.VERY_SLOW, restored.getGear())
        assertTrue(restored.advanceSchedule())
    }

    @Test
    fun `unknown persisted resource kind is ignored`() {
        val source = newReplicator()
        source.setFluidTemplate(FluidStack(Fluids.WATER, 1_000))
        val saved = source.saveWithFullMetadata(registries)
        saved.getCompound(ReplicatorBlockEntity.RESOURCE_TAG).putString("kind", "missing:resource")

        val restored = newReplicator()
        restored.loadWithComponents(saved, registries)

        assertNull(restored.getResource())
    }

    private fun newReplicator(): ReplicatorBlockEntity =
        ReplicatorBlockEntity(
            BlockPos.ZERO,
            ReplicatorRegistries.block.get().defaultBlockState(),
        )

    private data class TestVariant(
        val value: String,
    ) : ResourceVariant {
        override fun copyVariant(): ResourceVariant = copy()
    }

    private object TestResourceKind : ResourceKind<TestVariant> {
        override val id = ResourceLocation.fromNamespaceAndPath("lazy", "replicator_test")
        override val displayName: Component = Component.literal("Test")
        override val defaultAmount = 1_000L

        override fun variantName(variant: TestVariant): Component = Component.literal(variant.value)

        override fun matches(
            first: TestVariant,
            second: TestVariant,
        ): Boolean = first == second

        override fun copy(variant: TestVariant): TestVariant = variant.copy()

        override fun save(
            registries: net.minecraft.core.HolderLookup.Provider,
            variant: TestVariant,
        ): CompoundTag = CompoundTag().apply { putString("value", variant.value) }

        override fun parse(
            registries: net.minecraft.core.HolderLookup.Provider,
            tag: CompoundTag,
        ): TestVariant = TestVariant(tag.getString("value"))
    }
}
