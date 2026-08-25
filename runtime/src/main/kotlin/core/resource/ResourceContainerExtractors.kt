package rhx.lazy.core.resource

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import rhx.lazy.integration.api.LazyInternalApi

/**
 * Reads one resource identity from an item container without modifying the container.
 *
 * Extractors are tried in registration order. This gives built-in resources a deterministic
 * precedence while allowing optional integrations to append their own container capabilities.
 */
@LazyInternalApi
public interface ResourceContainerExtractor<V : ResourceVariant> {
    public val kind: ResourceKind<V>

    public fun extract(stack: ItemStack): V?
}

/** Registry used by resource selectors that explicitly request a contained resource. */
@LazyInternalApi
public object ResourceContainerExtractors {
    private val extractors = mutableListOf<ResourceContainerExtractor<out ResourceVariant>>()

    init {
        register(FluidContainerExtractor)
    }

    public fun register(extractor: ResourceContainerExtractor<out ResourceVariant>) {
        check(extractors.none { it.kind == extractor.kind }) {
            "A resource container extractor is already registered for ${extractor.kind.id}"
        }
        extractors += extractor
    }

    internal fun extractFirst(stack: ItemStack): ResourceAmount<out ResourceVariant>? {
        if (stack.isEmpty) return null
        return extractors.firstNotNullOfOrNull { extractor -> extract(extractor, stack.copyWithCount(1)) }
    }

    private fun <V : ResourceVariant> extract(
        extractor: ResourceContainerExtractor<V>,
        stack: ItemStack,
    ): ResourceAmount<V>? =
        extractor.extract(stack)?.let { variant ->
            ResourceAmount(extractor.kind, variant, extractor.kind.defaultAmount)
        }
}

private object FluidContainerExtractor : ResourceContainerExtractor<FluidVariant> {
    override val kind: ResourceKind<FluidVariant> = ResourceKinds.FLUID

    override fun extract(stack: ItemStack): FluidVariant? {
        val handler = stack.getCapability(Capabilities.FluidHandler.ITEM) ?: return null
        return (0 until handler.tanks)
            .firstNotNullOfOrNull { tank -> FluidVariant.of(handler.getFluidInTank(tank)) }
    }
}
