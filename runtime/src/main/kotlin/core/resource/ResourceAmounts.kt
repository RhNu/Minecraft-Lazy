package rhx.lazy.core.resource

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import rhx.lazy.integration.api.LazyInternalApi

/** A positive quantity paired with an immutable resource identity. */
@LazyInternalApi
public class ResourceAmount<V : ResourceVariant>(
    val kind: ResourceKind<V>,
    variant: V,
    val amount: Long,
) {
    private val storedVariant = kind.copy(variant)

    init {
        require(amount > 0L) { "A resource amount must be positive" }
    }

    val variant: V
        get() = kind.copy(storedVariant)

    val variantName: Component
        get() = kind.variantName(storedVariant)

    val sprite: ResourceSprite?
        get() = kind.sprite(storedVariant)

    fun matches(other: ResourceAmount<out ResourceVariant>): Boolean {
        if (kind !== other.kind) return false

        @Suppress("UNCHECKED_CAST")
        return kind.matches(storedVariant, (other as ResourceAmount<V>).storedVariant)
    }

    fun matches(
        expectedKind: ResourceKind<V>,
        expectedVariant: V,
    ): Boolean = kind === expectedKind && kind.matches(storedVariant, expectedVariant)

    fun withAmount(value: Long): ResourceAmount<V> = ResourceAmount(kind, storedVariant, value)

    fun copyAmount(): ResourceAmount<V> = ResourceAmount(kind, storedVariant, amount)

    fun save(registries: HolderLookup.Provider): CompoundTag =
        CompoundTag().apply {
            putString(KIND_TAG, kind.id.toString())
            put(VARIANT_TAG, kind.save(registries, storedVariant))
            putLong(AMOUNT_TAG, amount)
        }

    companion object {
        fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): ResourceAmount<out ResourceVariant>? {
            val kindId = ResourceLocation.tryParse(tag.getString(KIND_TAG)) ?: return null
            val kind = ResourceKinds[kindId] ?: return null
            val amount = tag.getLong(AMOUNT_TAG)
            if (amount <= 0L || !tag.contains(VARIANT_TAG, Tag.TAG_COMPOUND.toInt())) return null
            return parseUnchecked(registries, kind, tag.getCompound(VARIANT_TAG), amount)
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseUnchecked(
            registries: HolderLookup.Provider,
            kind: ResourceKind<out ResourceVariant>,
            variantTag: CompoundTag,
            amount: Long,
        ): ResourceAmount<out ResourceVariant>? {
            val typedKind = kind as ResourceKind<ResourceVariant>
            val variant = typedKind.parse(registries, variantTag) ?: return null
            return ResourceAmount(typedKind, variant, amount)
        }

        private const val KIND_TAG = "kind"
        private const val VARIANT_TAG = "variant"
        private const val AMOUNT_TAG = "amount"
    }
}

@LazyInternalApi
public fun itemAmount(
    stack: net.minecraft.world.item.ItemStack,
    amount: Long = stack.count.toLong(),
): ResourceAmount<ItemVariant>? = ItemVariant.of(stack)?.takeIf { amount > 0L }?.let { ResourceAmount(ItemResourceKind, it, amount) }

internal fun fluidAmount(
    stack: net.neoforged.neoforge.fluids.FluidStack,
    amount: Long = stack.amount.toLong(),
): ResourceAmount<FluidVariant>? = FluidVariant.of(stack)?.takeIf { amount > 0L }?.let { ResourceAmount(FluidResourceKind, it, amount) }

internal fun energyAmount(amount: Long): ResourceAmount<EnergyVariant>? =
    amount.takeIf { it > 0L }?.let { ResourceAmount(EnergyResourceKind, EnergyVariant, it) }

internal class ResourceBundle private constructor(
    amounts: List<ResourceAmount<out ResourceVariant>>,
) {
    private val storedAmounts = amounts.map(::copyAmount)

    val entries: List<ResourceAmount<out ResourceVariant>>
        get() = storedAmounts.map(::copyAmount)

    val isEmpty: Boolean
        get() = storedAmounts.isEmpty()

    companion object {
        val EMPTY = ResourceBundle(emptyList())

        fun of(amounts: Iterable<ResourceAmount<out ResourceVariant>>): ResourceBundle = ResourceBundle(amounts.toList())

        @Suppress("UNCHECKED_CAST")
        private fun copyAmount(amount: ResourceAmount<out ResourceVariant>): ResourceAmount<out ResourceVariant> {
            val typed = amount as ResourceAmount<ResourceVariant>
            return ResourceAmount(typed.kind, typed.variant, typed.amount)
        }
    }
}

internal data class ResourceDelta<V : ResourceVariant>(
    val extracted: List<ResourceAmount<V>> = emptyList(),
    val inserted: List<ResourceAmount<V>> = emptyList(),
)
