package rhx.lazy.core.resource

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

    fun matches(other: ResourceAmount<V>): Boolean = kind === other.kind && kind.matches(storedVariant, other.storedVariant)

    fun withAmount(value: Long): ResourceAmount<V> = ResourceAmount(kind, storedVariant, value)
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
