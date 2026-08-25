package rhx.lazy.core.resource

import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidType
import rhx.lazy.integration.api.LazyInternalApi

/**
 * An immutable identity for one resource. Quantities deliberately live in [ResourceAmount], never
 * inside an [ItemStack] or [FluidStack] whose count fields are limited to [Int].
 */
@LazyInternalApi
public interface ResourceVariant {
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

    /** Natural amount used when a UI marks this resource without an explicit quantity. */
    val defaultAmount: Long
        get() = 1L

    fun variantName(variant: V): Component

    /** Optional sprite used by generic resource selectors that cannot render the native stack type. */
    fun sprite(variant: V): ResourceSprite? = null

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

    override fun variantName(variant: ItemVariant): Component = variant.template.hoverName

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
    override val defaultAmount: Long = FluidType.BUCKET_VOLUME.toLong()

    override fun variantName(variant: FluidVariant): Component {
        val template = variant.template
        return template.get(DataComponents.CUSTOM_NAME) ?: template.hoverName
    }

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

    override fun variantName(variant: EnergyVariant): Component = displayName

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

/** Server-safe description of a tinted resource sprite. */
@LazyInternalApi
public data class ResourceSprite(
    val texture: ResourceLocation,
    val color: Int = -1,
)

/**
 * Runtime catalog for every resource identity understood by Lazy.
 *
 * The catalog is deliberately owned by the resource layer rather than IO. Features persist and
 * exchange resource amounts through this catalog, while integrations may register another kind
 * during common setup without teaching those features about the partner API.
 */
@LazyInternalApi
public object ResourceKinds {
    public val ITEM: ResourceKind<ItemVariant> = ItemResourceKind
    public val FLUID: ResourceKind<FluidVariant> = FluidResourceKind
    public val ENERGY: ResourceKind<EnergyVariant> = EnergyResourceKind

    private val kinds = linkedMapOf<ResourceLocation, ResourceKind<out ResourceVariant>>()

    init {
        register(ITEM)
        register(FLUID)
        register(ENERGY)
    }

    public val all: Set<ResourceKind<out ResourceVariant>>
        get() = kinds.values.toCollection(linkedSetOf())

    public fun register(kind: ResourceKind<out ResourceVariant>) {
        require(kind.defaultAmount > 0L) { "A resource kind's default amount must be positive: ${kind.id}" }
        check(kinds.putIfAbsent(kind.id, kind) == null) {
            "A resource kind is already registered for ${kind.id}"
        }
    }

    public operator fun get(id: ResourceLocation): ResourceKind<out ResourceVariant>? = kinds[id]
}
