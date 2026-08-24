package rhx.lazy.core.resource

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.integration.api.LazyInternalApi

/**
 * An immutable identity for one resource. Quantities deliberately live in [ResourceAmount], never
 * inside an [ItemStack] or [FluidStack] whose count fields are limited to [Int].
 */
@LazyInternalApi
public sealed interface ResourceVariant {
    fun copyVariant(): ResourceVariant
}

@LazyInternalApi
public class ItemVariant private constructor(
    stack: ItemStack,
) : ResourceVariant {
    private val storedTemplate = stack.copyWithCount(1)

    val template: ItemStack
        get() = storedTemplate.copy()

    fun matches(other: ItemVariant): Boolean = ItemStack.isSameItemSameComponents(storedTemplate, other.storedTemplate)

    override fun copyVariant(): ItemVariant = ItemVariant(storedTemplate)

    override fun toString(): String = storedTemplate.toString()

    companion object {
        fun of(stack: ItemStack): ItemVariant? = stack.takeUnless(ItemStack::isEmpty)?.let(::ItemVariant)
    }
}

@LazyInternalApi
public class FluidVariant private constructor(
    stack: FluidStack,
) : ResourceVariant {
    private val storedTemplate = stack.copyWithAmount(1)

    val template: FluidStack
        get() = storedTemplate.copy()

    fun matches(other: FluidVariant): Boolean = FluidStack.isSameFluidSameComponents(storedTemplate, other.storedTemplate)

    override fun copyVariant(): FluidVariant = FluidVariant(storedTemplate)

    override fun toString(): String = storedTemplate.toString()

    companion object {
        fun of(stack: FluidStack): FluidVariant? = stack.takeUnless(FluidStack::isEmpty)?.let(::FluidVariant)
    }
}

@LazyInternalApi
public data object EnergyVariant : ResourceVariant {
    override fun copyVariant(): ResourceVariant = this
}

/** Resource-specific identity and persistence operations used by stores and transfer providers. */
@LazyInternalApi
public interface ResourceKind<V : ResourceVariant> {
    val id: ResourceLocation
    val displayName: Component

    fun matches(
        first: V,
        second: V,
    ): Boolean

    fun copy(variant: V): V

    fun save(
        registries: HolderLookup.Provider,
        variant: V,
    ): CompoundTag

    fun parse(
        registries: HolderLookup.Provider,
        tag: CompoundTag,
    ): V?
}

@LazyInternalApi
public object ItemResourceKind : ResourceKind<ItemVariant> {
    override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lazy", "item")
    override val displayName: Component = Component.translatable("gui.lazy.io.capability.item")

    override fun matches(
        first: ItemVariant,
        second: ItemVariant,
    ): Boolean = first.matches(second)

    override fun copy(variant: ItemVariant): ItemVariant = variant.copyVariant()

    override fun save(
        registries: HolderLookup.Provider,
        variant: ItemVariant,
    ): CompoundTag = variant.template.save(registries) as CompoundTag

    override fun parse(
        registries: HolderLookup.Provider,
        tag: CompoundTag,
    ): ItemVariant? = ItemVariant.of(ItemStack.parseOptional(registries, tag))
}

@LazyInternalApi
public object FluidResourceKind : ResourceKind<FluidVariant> {
    override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lazy", "fluid")
    override val displayName: Component = Component.translatable("gui.lazy.io.capability.fluid")

    override fun matches(
        first: FluidVariant,
        second: FluidVariant,
    ): Boolean = first.matches(second)

    override fun copy(variant: FluidVariant): FluidVariant = variant.copyVariant()

    override fun save(
        registries: HolderLookup.Provider,
        variant: FluidVariant,
    ): CompoundTag {
        val encoded =
            FluidStack.CODEC
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), variant.template)
                .result()
                .orElse(null)
        return encoded as? CompoundTag ?: CompoundTag()
    }

    override fun parse(
        registries: HolderLookup.Provider,
        tag: CompoundTag,
    ): FluidVariant? =
        FluidStack.CODEC
            .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
            .result()
            .orElse(FluidStack.EMPTY)
            .let(FluidVariant::of)
}

@LazyInternalApi
public object EnergyResourceKind : ResourceKind<EnergyVariant> {
    override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("neoforge", "energy")
    override val displayName: Component = Component.translatable("gui.lazy.io.capability.energy")

    override fun matches(
        first: EnergyVariant,
        second: EnergyVariant,
    ) = true

    override fun copy(variant: EnergyVariant) = EnergyVariant

    override fun save(
        registries: HolderLookup.Provider,
        variant: EnergyVariant,
    ) = CompoundTag()

    override fun parse(
        registries: HolderLookup.Provider,
        tag: CompoundTag,
    ) = EnergyVariant
}
