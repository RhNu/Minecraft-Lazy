package rhx.lazy.integration.ae2

import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant

internal interface AeStoragePayloadAdapter {
    val kind: ResourceKind<out ResourceVariant>

    fun convert(amount: ResourceAmount<out ResourceVariant>): AeStoragePayload?
}

internal data class AeStoragePayload(
    val key: AEKey,
    val amount: Long,
)

internal object AeStoragePayloadAdapters {
    private val adapters = linkedMapOf<ResourceKind<out ResourceVariant>, AeStoragePayloadAdapter>()

    /** Snapshot rebuilt on registration; the router reads this on every push. */
    var capabilities: Set<ResourceKind<out ResourceVariant>> = emptySet()
        private set

    fun register(adapter: AeStoragePayloadAdapter) {
        check(adapters.putIfAbsent(adapter.kind, adapter) == null) {
            "An AE storage adapter is already registered for ${adapter.kind.id}"
        }
        capabilities = adapters.keys.toSet()
    }

    fun convert(amount: ResourceAmount<out ResourceVariant>): AeStoragePayload? = adapters[amount.kind]?.convert(amount)

    fun registerAe2Adapters() {
        register(
            object : AeStoragePayloadAdapter {
                override val kind = ItemResourceKind

                override fun convert(amount: ResourceAmount<out ResourceVariant>): AeStoragePayload? {
                    val variant = amount.variant as? ItemVariant ?: return null
                    val key = AEItemKey.of(variant.template) ?: return null
                    return AeStoragePayload(key, amount.amount)
                }
            },
        )
        register(
            object : AeStoragePayloadAdapter {
                override val kind = FluidResourceKind

                override fun convert(amount: ResourceAmount<out ResourceVariant>): AeStoragePayload? {
                    val variant = amount.variant as? FluidVariant ?: return null
                    val key = AEFluidKey.of(variant.template) ?: return null
                    return AeStoragePayload(key, amount.amount)
                }
            },
        )
    }
}
