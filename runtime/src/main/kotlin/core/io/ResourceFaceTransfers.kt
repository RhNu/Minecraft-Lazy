package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.api.LazyInternalApi
import java.util.function.BooleanSupplier

/** Per-machine transfer state for one resource kind and its neighbour capability. */
@LazyInternalApi
public interface ResourceFaceTransfer<V : ResourceVariant> {
    fun offer(
        level: ServerLevel,
        direction: Direction,
        variant: V,
        amount: Long,
    ): Long

    fun invalidate()
}

/** Creates the per-machine state needed to push one extensible resource kind to adjacent blocks. */
@LazyInternalApi
public interface ResourceFaceTransferFactory<V : ResourceVariant> {
    val kind: ResourceKind<V>

    fun create(
        origin: BlockPos,
        stillValid: BooleanSupplier,
    ): ResourceFaceTransfer<V>
}

@LazyInternalApi
public object ResourceFaceTransferFactories {
    private val factories = linkedMapOf<ResourceKind<out ResourceVariant>, ResourceFaceTransferFactory<out ResourceVariant>>()

    public fun register(factory: ResourceFaceTransferFactory<out ResourceVariant>) {
        check(factories.putIfAbsent(factory.kind, factory) == null) {
            "A face transfer factory is already registered for ${factory.kind.id}"
        }
    }

    internal fun create(
        kind: ResourceKind<out ResourceVariant>,
        origin: BlockPos,
        stillValid: BooleanSupplier,
    ): UntypedResourceFaceTransfer? = factories[kind]?.let { createUnchecked(it, origin, stillValid) }

    @Suppress("UNCHECKED_CAST")
    private fun createUnchecked(
        factory: ResourceFaceTransferFactory<out ResourceVariant>,
        origin: BlockPos,
        stillValid: BooleanSupplier,
    ): UntypedResourceFaceTransfer {
        val typedFactory = factory as ResourceFaceTransferFactory<ResourceVariant>
        return UntypedResourceFaceTransfer(typedFactory.kind, typedFactory.create(origin, stillValid))
    }
}

internal class UntypedResourceFaceTransfer(
    private val kind: ResourceKind<ResourceVariant>,
    private val transfer: ResourceFaceTransfer<ResourceVariant>,
) {
    fun offer(
        level: ServerLevel,
        direction: Direction,
        amount: ResourceAmount<out ResourceVariant>,
    ): Long {
        if (amount.kind !== kind) return 0L

        @Suppress("UNCHECKED_CAST")
        val typedAmount = amount as ResourceAmount<ResourceVariant>
        return transfer.offer(level, direction, typedAmount.variant, amount.amount).coerceIn(0L, amount.amount)
    }

    fun invalidate() {
        transfer.invalidate()
    }
}
